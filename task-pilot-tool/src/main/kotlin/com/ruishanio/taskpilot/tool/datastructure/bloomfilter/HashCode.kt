package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import com.ruishanio.taskpilot.tool.core.AssertTool
import java.io.Serializable
import kotlin.math.abs
import kotlin.math.min

/**
 * 哈希码抽象。
 * 继续拆成 int/long/bytes 三种实现，避免位宽不一致时出现多余的数组复制。
 */
abstract class HashCode {
    abstract fun bits(): Int

    abstract fun asInt(): Int

    abstract fun asLong(): Long

    abstract fun padToLong(): Long

    abstract fun asBytes(): ByteArray

    open fun getBytesInternal(): ByteArray = asBytes()

    protected abstract fun equalsSameBits(that: HashCode): Boolean

    final override fun equals(other: Any?): Boolean {
        if (other is HashCode) {
            return bits() == other.bits() && equalsSameBits(other)
        }
        return false
    }

    final override fun hashCode(): Int {
        if (bits() >= 32) {
            return asInt()
        }

        val bytes = getBytesInternal()
        var value = bytes[0].toInt() and 0xFF
        for (i in 1 until bytes.size) {
            value = value or ((bytes[i].toInt() and 0xFF) shl (i * 8))
        }
        return value
    }

    final override fun toString(): String {
        val bytes = getBytesInternal()
        val stringBuilder = StringBuilder(2 * bytes.size)
        for (b in bytes) {
            stringBuilder.append(HEX_DIGITS[(b.toInt() shr 4) and 0xF]).append(HEX_DIGITS[b.toInt() and 0xF])
        }
        return stringBuilder.toString()
    }

    private class IntHashCode(
        private val hash: Int,
    ) : HashCode(), Serializable {
        override fun bits(): Int = 32

        override fun asBytes(): ByteArray =
            byteArrayOf(
                hash.toByte(),
                (hash shr 8).toByte(),
                (hash shr 16).toByte(),
                (hash shr 24).toByte(),
            )

        override fun asInt(): Int = hash

        override fun asLong(): Long = throw IllegalStateException("this HashCode only has 32 bits; cannot create a long")

        override fun padToLong(): Long = hash.toLong() and INT_MASK

        override fun equalsSameBits(that: HashCode): Boolean = hash == that.asInt()
    }

    private class LongHashCode(
        private val hash: Long,
    ) : HashCode(), Serializable {
        override fun bits(): Int = 64

        override fun asBytes(): ByteArray =
            byteArrayOf(
                hash.toByte(),
                (hash shr 8).toByte(),
                (hash shr 16).toByte(),
                (hash shr 24).toByte(),
                (hash shr 32).toByte(),
                (hash shr 40).toByte(),
                (hash shr 48).toByte(),
                (hash shr 56).toByte(),
            )

        override fun asInt(): Int = hash.toInt()

        override fun asLong(): Long = hash

        override fun padToLong(): Long = hash

        override fun equalsSameBits(that: HashCode): Boolean = hash == that.asLong()
    }

    private class BytesHashCode(
        private val bytes: ByteArray,
    ) : HashCode(), Serializable {
        init {
            AssertTool.notNull(bytes, "bytes must not be null")
        }

        override fun bits(): Int = bytes.size * 8

        override fun asBytes(): ByteArray = bytes.clone()

        override fun asInt(): Int {
            AssertTool.isTrue(bytes.size >= 4, "HashCode#asInt() requires >= 4 bytes (it only has ${bytes.size} bytes).")
            return (bytes[0].toInt() and 0xFF) or
                ((bytes[1].toInt() and 0xFF) shl 8) or
                ((bytes[2].toInt() and 0xFF) shl 16) or
                ((bytes[3].toInt() and 0xFF) shl 24)
        }

        override fun asLong(): Long {
            AssertTool.isTrue(bytes.size >= 8, "HashCode#asLong() requires >= 8 bytes (it only has ${bytes.size} bytes).")
            return padToLong()
        }

        override fun padToLong(): Long {
            var returnValue = bytes[0].toLong() and 0xFF
            for (i in 1 until min(bytes.size, 8)) {
                returnValue = returnValue or ((bytes[i].toLong() and 0xFFL) shl (i * 8))
            }
            return returnValue
        }

        override fun getBytesInternal(): ByteArray = bytes

        override fun equalsSameBits(that: HashCode): Boolean {
            val thatBytes = that.getBytesInternal()
            if (bytes.size != thatBytes.size) {
                return false
            }

            var areEqual = true
            for (i in bytes.indices) {
                areEqual = areEqual and (bytes[i] == thatBytes[i])
            }
            return areEqual
        }
    }

    companion object {
        private val HEX_DIGITS = "0123456789abcdef".toCharArray()
        private const val INT_MASK: Long = 0xffffffffL
        fun fromInt(hash: Int): HashCode = IntHashCode(hash)
        fun fromLong(hash: Long): HashCode = LongHashCode(hash)
        fun fromBytes(bytes: ByteArray): HashCode {
            AssertTool.isTrue(bytes.isNotEmpty(), "A HashCode must contain at least 1 byte.")
            return fromBytesNoCopy(bytes.clone())
        }
        fun fromBytesNoCopy(bytes: ByteArray): HashCode = BytesHashCode(bytes)
    }
}
