package com.ruishanio.taskpilot.tool.concurrent

import com.ruishanio.taskpilot.tool.core.AssertTool
import java.time.Duration
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * 令牌桶。
 * 继续沿用原有的平滑突发/预热实现，避免在限流边界条件上和既有压测结果产生偏差。
 */
abstract class TokenBucket protected constructor() {
    protected var storedPermits: Double = 0.0
    protected var maxPermits: Double = 0.0
    protected var stableIntervalMicros: Double = 0.0
    protected var permitsPerSecond: Double = 0.0

    private var nextFreeTicketMicros: Long = 0L
    private val stopwatch = Stopwatch.createStarted()

    @Volatile
    private var mutexDoNotUseDirectly: Any? = null

    private fun mutex(): Any {
        var mutex = mutexDoNotUseDirectly
        if (mutex == null) {
            synchronized(this) {
                mutex = mutexDoNotUseDirectly
                if (mutex == null) {
                    mutex = Any()
                    mutexDoNotUseDirectly = mutex
                }
            }
        }
        return mutex!!
    }

    fun setRate(permitsPerSecond: Double) {
        AssertTool.isTrue(permitsPerSecond > 0.0, "rate must be positive")
        synchronized(mutex()) {
            resync(stopwatch.elapsed(TimeUnit.MICROSECONDS))
            this.permitsPerSecond = permitsPerSecond
            this.stableIntervalMicros = TimeUnit.SECONDS.toMicros(1L).toDouble() / permitsPerSecond
            doSetRate()
        }
    }

    protected abstract fun doSetRate()

    protected fun resync(nowMicros: Long) {
        if (nowMicros > nextFreeTicketMicros) {
            val newPermits = (nowMicros - nextFreeTicketMicros) / coolDownIntervalMicros()
            storedPermits = min(maxPermits, storedPermits + newPermits)
            nextFreeTicketMicros = nowMicros
        }
    }

    protected abstract fun storedPermitsToWaitTime(permitsToTake: Double): Long

    protected abstract fun coolDownIntervalMicros(): Double

    fun acquire(): Double = acquire(1)

    fun acquire(permits: Int): Double {
        val microsToWait = reserve(permits)
        if (microsToWait > 0) {
            sleepUninterruptibly(microsToWait, TimeUnit.MICROSECONDS)
        }
        return 1.0 * microsToWait / TimeUnit.SECONDS.toMicros(1L)
    }

    fun reserve(permits: Int): Long {
        AssertTool.isTrue(permits > 0, "Requested permits must be positive, permits:$permits")
        synchronized(mutex()) {
            return reserveAndGetWaitLength(permits, stopwatch.elapsed(TimeUnit.MICROSECONDS))
        }
    }

    fun tryAcquire(): Boolean = tryAcquire(1, 0, TimeUnit.MICROSECONDS)

    fun tryAcquire(timeout: Duration): Boolean = tryAcquire(1, toNanosSaturated(timeout), TimeUnit.NANOSECONDS)

    fun tryAcquire(timeout: Long, unit: TimeUnit): Boolean = tryAcquire(1, timeout, unit)

    fun tryAcquire(permits: Int): Boolean = tryAcquire(permits, 0, TimeUnit.MICROSECONDS)

    fun tryAcquire(
        permits: Int,
        timeout: Duration,
    ): Boolean = tryAcquire(permits, toNanosSaturated(timeout), TimeUnit.NANOSECONDS)

    fun tryAcquire(
        permits: Int,
        timeout: Long,
        unit: TimeUnit,
    ): Boolean {
        val timeoutMicros = kotlin.math.max(unit.toMicros(timeout), 0)
        AssertTool.isTrue(permits > 0, "Requested permits must be positive, permits:$permits")

        val microsToWait: Long
        synchronized(mutex()) {
            val nowMicros = stopwatch.elapsed(TimeUnit.MICROSECONDS)
            if (!canAcquire(nowMicros, timeoutMicros)) {
                return false
            }
            microsToWait = reserveAndGetWaitLength(permits, nowMicros)
        }

        if (microsToWait > 0) {
            sleepUninterruptibly(microsToWait, TimeUnit.MICROSECONDS)
        }
        return true
    }

    private fun canAcquire(
        nowMicros: Long,
        timeoutMicros: Long,
    ): Boolean = nextFreeTicketMicros <= nowMicros + timeoutMicros

    private fun reserveAndGetWaitLength(
        permits: Int,
        nowMicros: Long,
    ): Long {
        val momentAvailable = reserveEarliestAvailable(permits, nowMicros)
        return kotlin.math.max(momentAvailable - nowMicros, 0)
    }

    /**
     * 预占许可时继续区分“库存令牌”和“新发令牌”的等待成本，保持平滑限流曲线。
     */
    private fun reserveEarliestAvailable(
        requiredPermits: Int,
        nowMicros: Long,
    ): Long {
        resync(nowMicros)
        val returnValue = nextFreeTicketMicros
        val storedPermitsToSpend = min(requiredPermits.toDouble(), storedPermits)
        val freshPermits = requiredPermits - storedPermitsToSpend
        val waitMicros = storedPermitsToWaitTime(storedPermitsToSpend) + (freshPermits * stableIntervalMicros).toLong()

        nextFreeTicketMicros = saturatedAdd(nextFreeTicketMicros, waitMicros)
        storedPermits -= storedPermitsToSpend
        return returnValue
    }

    override fun toString(): String = String.format(Locale.ROOT, "TokenBucket[stableRate=%3.1fqps]", permitsPerSecond)

    private class SmoothBursty(
        private val maxBurstSeconds: Double,
    ) : TokenBucket() {
        override fun doSetRate() {
            val oldMaxPermits = maxPermits
            maxPermits = maxBurstSeconds * permitsPerSecond
            storedPermits =
                if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                    maxPermits
                } else {
                    if (oldMaxPermits == 0.0) 0.0 else storedPermits * maxPermits / oldMaxPermits
                }
        }

        override fun storedPermitsToWaitTime(permitsToTake: Double): Long = 0L

        override fun coolDownIntervalMicros(): Double = stableIntervalMicros
    }

    private class SmoothWarmingUp(
        warmupPeriod: Long,
        timeUnit: TimeUnit,
        private val coldFactor: Double,
    ) : TokenBucket() {
        private val warmupPeriodMicros: Long = timeUnit.toMicros(warmupPeriod)
        private var slope: Double = 0.0
        private var thresholdPermits: Double = 0.0

        override fun doSetRate() {
            val oldMaxPermits = maxPermits
            val coldIntervalMicros = stableIntervalMicros * coldFactor
            thresholdPermits = 0.5 * warmupPeriodMicros / stableIntervalMicros
            maxPermits = thresholdPermits + 2.0 * warmupPeriodMicros / (stableIntervalMicros + coldIntervalMicros)
            slope = (coldIntervalMicros - stableIntervalMicros) / (maxPermits - thresholdPermits)
            storedPermits =
                if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                    0.0
                } else {
                    if (oldMaxPermits == 0.0) maxPermits else storedPermits * maxPermits / oldMaxPermits
                }
        }

        override fun storedPermitsToWaitTime(permitsToTake: Double): Long {
            var finalPermitsToTake = permitsToTake
            val availablePermitsAboveThreshold = storedPermits - thresholdPermits
            var micros = 0L
            if (availablePermitsAboveThreshold > 0.0) {
                val permitsAboveThresholdToTake = min(availablePermitsAboveThreshold, finalPermitsToTake)
                val length =
                    permitsToTime(availablePermitsAboveThreshold) +
                        permitsToTime(availablePermitsAboveThreshold - permitsAboveThresholdToTake)
                micros = (permitsAboveThresholdToTake * length / 2.0).toLong()
                finalPermitsToTake -= permitsAboveThresholdToTake
            }
            micros += (stableIntervalMicros * finalPermitsToTake).toLong()
            return micros
        }

        private fun permitsToTime(permits: Double): Double = stableIntervalMicros + permits * slope

        override fun coolDownIntervalMicros(): Double = warmupPeriodMicros.toDouble() / maxPermits
    }

    /**
     * 秒表独立成内部类型，便于限流算法和测试共用同一套时间换算逻辑。
     */
    class Stopwatch private constructor() {
        private var isRunning: Boolean = false
        private var elapsedNanos: Long = 0L
        private var startTick: Long = 0L

        fun isRunning(): Boolean = isRunning

        fun start(): Stopwatch {
            AssertTool.isTrue(!isRunning, "This stopwatch is already running.")
            isRunning = true
            startTick = System.nanoTime()
            return this
        }

        fun stop(): Stopwatch {
            AssertTool.isTrue(isRunning, "This stopwatch is already stopped.")
            isRunning = false
            elapsedNanos += System.nanoTime() - startTick
            return this
        }

        fun reset(): Stopwatch {
            elapsedNanos = 0
            isRunning = false
            return this
        }

        private fun elapsedNanosValue(): Long = if (isRunning) System.nanoTime() - startTick + elapsedNanos else elapsedNanos

        fun elapsed(desiredUnit: TimeUnit): Long = desiredUnit.convert(elapsedNanosValue(), TimeUnit.NANOSECONDS)

        fun elapsed(): Duration = Duration.ofNanos(elapsedNanosValue())

        override fun toString(): String {
            val nanos = elapsedNanosValue()
            val unit = chooseUnit(nanos)
            val value = nanos.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit)
            return formatCompact4Digits(value) + " " + abbreviate(unit)
        }

        companion object {
            fun createUnstarted(): Stopwatch = Stopwatch()
            fun createStarted(): Stopwatch = Stopwatch().start()

            private fun chooseUnit(nanos: Long): TimeUnit {
                if (TimeUnit.DAYS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                    return TimeUnit.DAYS
                }
                if (TimeUnit.HOURS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                    return TimeUnit.HOURS
                }
                if (TimeUnit.MINUTES.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                    return TimeUnit.MINUTES
                }
                if (TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                    return TimeUnit.SECONDS
                }
                if (TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                    return TimeUnit.MILLISECONDS
                }
                if (TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS) > 0) {
                    return TimeUnit.MICROSECONDS
                }
                return TimeUnit.NANOSECONDS
            }

            private fun abbreviate(unit: TimeUnit): String =
                when (unit) {
                    TimeUnit.NANOSECONDS -> "ns"
                    TimeUnit.MICROSECONDS -> "\u03bcs"
                    TimeUnit.MILLISECONDS -> "ms"
                    TimeUnit.SECONDS -> "s"
                    TimeUnit.MINUTES -> "min"
                    TimeUnit.HOURS -> "h"
                    TimeUnit.DAYS -> "d"
                }
        }
    }

    companion object {
        fun create(permitsPerSecond: Double): TokenBucket {
            val tokenBucket: TokenBucket = SmoothBursty(1.0)
            tokenBucket.setRate(permitsPerSecond)
            return tokenBucket
        }
        fun create(
            permitsPerSecond: Double,
            warmupPeriod: Duration,
        ): TokenBucket = create(permitsPerSecond, toNanosSaturated(warmupPeriod), TimeUnit.NANOSECONDS)
        fun create(
            permitsPerSecond: Double,
            warmupPeriod: Long,
            unit: TimeUnit,
        ): TokenBucket {
            AssertTool.isTrue(warmupPeriod >= 0, "warmupPeriod must not be negative: $warmupPeriod")
            return create(permitsPerSecond, warmupPeriod, unit, 3.0)
        }
        fun create(
            permitsPerSecond: Double,
            warmupPeriod: Long,
            unit: TimeUnit,
            coldFactor: Double,
        ): TokenBucket {
            val tokenBucket: TokenBucket = SmoothWarmingUp(warmupPeriod, unit, coldFactor)
            tokenBucket.setRate(permitsPerSecond)
            return tokenBucket
        }
        fun toNanosSaturated(duration: Duration): Long {
            return try {
                duration.toNanos()
            } catch (_: ArithmeticException) {
                if (duration.isNegative) Long.MIN_VALUE else Long.MAX_VALUE
            }
        }
        fun formatCompact4Digits(value: Double): String = String.format(Locale.ROOT, "%.4g", value)

        /**
         * 休眠工具继续吞掉中断并在最后补发中断标记，保持限流等待阶段的可预期性。
         */
        fun sleepUninterruptibly(
            sleepFor: Long,
            unit: TimeUnit,
        ) {
            var interrupted = false
            try {
                var remainingNanos = unit.toNanos(sleepFor)
                val end = System.nanoTime() + remainingNanos
                while (true) {
                    try {
                        if (remainingNanos <= 0) {
                            return
                        }
                        TimeUnit.NANOSECONDS.sleep(remainingNanos)
                        return
                    } catch (_: InterruptedException) {
                        interrupted = true
                        remainingNanos = end - System.nanoTime()
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        fun saturatedAdd(
            a: Long,
            b: Long,
        ): Long {
            val naiveSum = a + b
            if ((a xor b) < 0 || (a xor naiveSum) >= 0) {
                return naiveSum
            }
            return Long.MAX_VALUE + ((naiveSum ushr (Long.SIZE_BITS - 1)) xor 1)
        }
    }
}
