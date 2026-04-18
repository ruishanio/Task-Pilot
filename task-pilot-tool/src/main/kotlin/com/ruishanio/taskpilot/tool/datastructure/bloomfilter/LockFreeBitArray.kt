package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import com.ruishanio.taskpilot.tool.core.AssertTool
import java.util.Arrays
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.LongAdder

/**
 * 无锁位数组。
 * 继续用 CAS + LongAdder 维护位状态和计数，避免 BloomFilter 在并发场景退化成粗粒度锁。
 */
class LockFreeBitArray {
    private val data: AtomicLongArray
    private val bitCount: LongAdder = LongAdder()

    constructor(bits: Long) {
        AssertTool.isTrue(bits > 0, "data length is zero!")

        val lengthL = longDivide(bits, 64)
        val length = lengthL.toInt()
        AssertTool.isTrue(length.toLong() == lengthL, "data length is not a multiple of 64 bits!")

        data = AtomicLongArray(length)
    }

    constructor(data: LongArray) {
        AssertTool.isTrue(data.isNotEmpty(), "data length is zero!")
        this.data = AtomicLongArray(data)

        var finalBitCount = 0L
        for (value in data) {
            finalBitCount += java.lang.Long.bitCount(value)
        }
        bitCount.add(finalBitCount)
    }

    fun set(bitIndex: Long): Boolean {
        if (get(bitIndex)) {
            return false
        }

        val longIndex = (bitIndex ushr LONG_ADDRESSABLE_BITS).toInt()
        val mask = 1L shl bitIndex.toInt()
        var oldValue: Long
        var newValue: Long
        do {
            oldValue = data.get(longIndex)
            newValue = oldValue or mask
            if (oldValue == newValue) {
                return false
            }
        } while (!data.compareAndSet(longIndex, oldValue, newValue))

        bitCount.increment()
        return true
    }

    fun get(bitIndex: Long): Boolean = (data.get((bitIndex ushr LONG_ADDRESSABLE_BITS).toInt()) and (1L shl bitIndex.toInt())) != 0L

    fun bitSize(): Long = data.length().toLong() * Long.SIZE_BITS

    fun bitCount(): Long = bitCount.sum()

    fun copy(): LockFreeBitArray = LockFreeBitArray(toPlainArray(data))

    /**
     * 合并时继续按位或写入，并且只在真实新增 bit 时补计数，避免统计值漂移。
     */
    fun putAll(other: LockFreeBitArray) {
        AssertTool.isTrue(data.length() == other.data.length(), "BitArrays must be of equal length (${data.length()} != ${other.data.length()})")
        for (i in 0 until data.length()) {
            putData(i, other.data.get(i))
        }
    }

    fun putData(
        i: Int,
        longValue: Long,
    ) {
        var ourLongOld: Long
        var ourLongNew: Long
        var changedAnyBits = true
        do {
            ourLongOld = data.get(i)
            ourLongNew = ourLongOld or longValue
            if (ourLongOld == ourLongNew) {
                changedAnyBits = false
                break
            }
        } while (!data.compareAndSet(i, ourLongOld, ourLongNew))

        if (changedAnyBits) {
            val bitsAdded = java.lang.Long.bitCount(ourLongNew) - java.lang.Long.bitCount(ourLongOld)
            bitCount.add(bitsAdded.toLong())
        }
    }

    fun dataLength(): Int = data.length()

    override fun equals(other: Any?): Boolean {
        if (other is LockFreeBitArray) {
            return Arrays.equals(toPlainArray(data), toPlainArray(other.data))
        }
        return false
    }

    override fun hashCode(): Int = Arrays.hashCode(toPlainArray(data))

    companion object {
        private const val LONG_ADDRESSABLE_BITS: Int = 6
        fun toPlainArray(atomicLongArray: AtomicLongArray): LongArray {
            val array = LongArray(atomicLongArray.length())
            for (i in array.indices) {
                array[i] = atomicLongArray.get(i)
            }
            return array
        }

        private fun longDivide(
            p: Long,
            q: Long,
        ): Long {
            val div = p / q
            val rem = p - q * div
            if (rem == 0L) {
                return div
            }

            val signum = 1 or ((p xor q) shr (Long.SIZE_BITS - 1)).toInt()
            return if (signum > 0) div + signum else div
        }
    }
}
