package com.ruishanio.taskpilot.tool.datastructure

import com.ruishanio.taskpilot.tool.core.AssertTool
import com.ruishanio.taskpilot.tool.datastructure.bloomfilter.Funnels
import com.ruishanio.taskpilot.tool.datastructure.bloomfilter.HashStrategies
import com.ruishanio.taskpilot.tool.datastructure.bloomfilter.LockFreeBitArray
import java.io.Serializable
import java.util.Objects
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

/**
 * 布隆过滤器。
 * 继续保留可合并、可估算元素数量的实现，兼容现有工具测试和上层对误判率的预期。
 */
class BloomFilter<T : Any> private constructor(
    private val bitArray: LockFreeBitArray,
    private val hashFunctionCount: Int,
    private val funnel: Funnels.Funnel<T>,
    private val strategy: HashStrategies.Strategy,
) : Serializable {
    init {
        AssertTool.isTrue(hashFunctionCount > 0, "hashFunctionCount ($hashFunctionCount) must be > 0")
        AssertTool.isTrue(hashFunctionCount <= 255, "hashFunctionCount ($hashFunctionCount) must be <= 255")
        AssertTool.notNull(bitArray, "bits must not be null")
        AssertTool.notNull(funnel, "funnel must not be null")
        AssertTool.notNull(strategy, "strategy must not be null")
    }

    fun mightContain(`object`: T): Boolean = strategy.mightContain(`object`, funnel, hashFunctionCount, bitArray)

    fun put(`object`: T): Boolean = strategy.put(`object`, funnel, hashFunctionCount, bitArray)

    fun expectedFpp(): Double = (bitArray.bitCount().toDouble() / bitSize()).pow(hashFunctionCount.toDouble())

    fun approximateElementCount(): Long {
        val bitSize = bitArray.bitSize()
        val bitCount = bitArray.bitCount()
        val fractionOfBitsSet = bitCount.toDouble() / bitSize
        return roundToLong(-kotlin.math.ln1p(-fractionOfBitsSet) * bitSize / hashFunctionCount)
    }

    fun bitSize(): Long = bitArray.bitSize()

    fun isCompatible(that: BloomFilter<T>): Boolean {
        AssertTool.notNull(this, "BloomFilter must not be null")
        return this !== that &&
            this.hashFunctionCount == that.hashFunctionCount &&
            this.bitSize() == that.bitSize() &&
            this.strategy == that.strategy &&
            this.funnel == that.funnel
    }

    /**
     * 合并前继续做完整兼容性校验，防止不同参数的布隆过滤器被误并到一起。
     */
    fun putAll(that: BloomFilter<T>) {
        AssertTool.notNull(that, "BloomFilter must not be null")
        AssertTool.isTrue(this !== that, "Cannot combine a BloomFilter with itself.")
        AssertTool.isTrue(
            this.hashFunctionCount == that.hashFunctionCount,
            "BloomFilters must have the same number of hash functions (${this.hashFunctionCount} != ${that.hashFunctionCount})",
        )
        AssertTool.isTrue(
            this.bitSize() == that.bitSize(),
            "BloomFilters must have the same size underlying bit arrays (${this.bitSize()} != ${that.bitSize()})",
        )
        AssertTool.isTrue(this.strategy == that.strategy, "BloomFilters must have equal strategies (${this.strategy} != ${that.strategy})")
        AssertTool.isTrue(this.funnel == that.funnel, "BloomFilters must have equal funnels (${this.funnel} != ${that.funnel})")
        this.bitArray.putAll(that.bitArray)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is BloomFilter<*>) {
            return this.hashFunctionCount == other.hashFunctionCount &&
                this.funnel == other.funnel &&
                this.bitArray == other.bitArray &&
                this.strategy == other.strategy
        }
        return false
    }

    override fun hashCode(): Int = Objects.hash(hashFunctionCount, funnel, strategy, bitArray)

    companion object {
        private const val serialVersionUID: Long = 42L
        private val LOG_TWO: Double = ln(2.0)
        private val SQUARED_LOG_TWO: Double = LOG_TWO * LOG_TWO
        private const val MIN_LONG_AS_DOUBLE: Double = -9.223372036854776E18
        private const val MAX_LONG_AS_DOUBLE_PLUS_ONE: Double = 9.223372036854776E18
        fun create(size: Long): BloomFilter<String> = create(Funnels.STRING, size, 0.03)
        fun create(
            expectedInsertions: Long,
            fpp: Double,
        ): BloomFilter<String> = create(Funnels.STRING, expectedInsertions, fpp, HashStrategies.MURMURHASH3_128_MITZ_64)
        fun <T : Any> create(
            funnel: Funnels.Funnel<T>,
            expectedInsertions: Long,
        ): BloomFilter<T> = create(funnel, expectedInsertions, 0.03)
        fun <T : Any> create(
            funnel: Funnels.Funnel<T>,
            size: Long,
            fpp: Double,
        ): BloomFilter<T> = create(funnel, size, fpp, HashStrategies.MURMURHASH3_128_MITZ_64)
        fun <T : Any> create(
            funnel: Funnels.Funnel<T>,
            size: Long,
            fpp: Double,
            strategy: HashStrategies.Strategy,
        ): BloomFilter<T> {
            AssertTool.notNull(funnel, "funnel must not be null")
            AssertTool.isTrue(size >= 0, "Expected insertions ($size) must be >= 0")
            AssertTool.isTrue(fpp > 0.0, "False positive probability ($fpp) must be > 0.0")
            AssertTool.isTrue(fpp < 1.0, "False positive probability ($fpp) must be < 1.0")
            AssertTool.notNull(strategy, "strategy must not be null")

            val finalSize = if (size <= 0) 1 else size
            val bitCount = optimalNumOfBits(finalSize, fpp)
            val hashFunctionCount = optimalNumOfHashFunctions(fpp)

            return try {
                BloomFilter(LockFreeBitArray(bitCount), hashFunctionCount, funnel, strategy)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Could not create BloomFilter of $bitCount bits", e)
            }
        }

        private fun optimalNumOfHashFunctions(p: Double): Int = max(1, kotlin.math.round(-ln(p) / LOG_TWO).toInt())

        private fun optimalNumOfBits(
            n: Long,
            p: Double,
        ): Long {
            val finalP = if (p == 0.0) Double.MIN_VALUE else p
            return (-n * ln(finalP) / SQUARED_LOG_TWO).toLong()
        }
        fun roundToLong(x: Double): Long {
            val z = roundIntermediate(x)
            AssertTool.isTrue(MIN_LONG_AS_DOUBLE - z < 1.0 && z < MAX_LONG_AS_DOUBLE_PLUS_ONE, "rounded value is out of range for input $x")
            return z.toLong()
        }

        private fun roundIntermediate(x: Double): Double {
            if (!isFinite(x)) {
                throw ArithmeticException("input is infinite or NaN")
            }
            val z = Math.rint(x)
            return if (abs(x - z) == 0.5) x + Math.copySign(0.5, x) else z
        }

        private fun isFinite(d: Double): Boolean = Math.getExponent(d) <= Math.getExponent(Double.MAX_VALUE)
    }
}
