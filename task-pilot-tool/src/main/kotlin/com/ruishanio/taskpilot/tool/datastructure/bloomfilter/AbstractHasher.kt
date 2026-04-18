package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * 抽象哈希器。
 * 继续提供基础类型和对象 funnel 的统一写入口，让具体哈希算法只关注状态更新。
 */
abstract class AbstractHasher {
    abstract fun putByte(b: Byte): AbstractHasher

    open fun putBytes(bytes: ByteArray): AbstractHasher = putBytes(bytes, 0, bytes.size)

    open fun putBytes(
        bytes: ByteArray,
        off: Int,
        len: Int,
    ): AbstractHasher {
        checkPositionIndexes(off, off + len, bytes.size)
        for (i in 0 until len) {
            putByte(bytes[off + i])
        }
        return this
    }

    open fun putBytes(byteBuffer: ByteBuffer): AbstractHasher {
        if (byteBuffer.hasArray()) {
            putBytes(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining())
            byteBuffer.position(byteBuffer.limit())
        } else {
            var remaining = byteBuffer.remaining()
            while (remaining > 0) {
                putByte(byteBuffer.get())
                remaining--
            }
        }
        return this
    }

    open fun putShort(s: Short): AbstractHasher {
        putByte(s.toByte())
        putByte((s.toInt() ushr 8).toByte())
        return this
    }

    open fun putInt(i: Int): AbstractHasher {
        putByte(i.toByte())
        putByte((i ushr 8).toByte())
        putByte((i ushr 16).toByte())
        putByte((i ushr 24).toByte())
        return this
    }

    open fun putLong(l: Long): AbstractHasher {
        var shift = 0
        while (shift < 64) {
            putByte((l ushr shift).toByte())
            shift += 8
        }
        return this
    }

    fun putFloat(f: Float): AbstractHasher = putInt(java.lang.Float.floatToRawIntBits(f))

    fun putDouble(d: Double): AbstractHasher = putLong(java.lang.Double.doubleToRawLongBits(d))

    fun putBoolean(b: Boolean): AbstractHasher = putByte(if (b) 1 else 0)

    open fun putChar(c: Char): AbstractHasher {
        putByte(c.code.toByte())
        putByte((c.code ushr 8).toByte())
        return this
    }

    fun putUnencodedChars(charSequence: CharSequence): AbstractHasher {
        for (i in 0 until charSequence.length) {
            putChar(charSequence[i])
        }
        return this
    }

    fun putString(
        charSequence: CharSequence,
        charset: Charset,
    ): AbstractHasher = putBytes(charSequence.toString().toByteArray(charset))

    fun <T : Any> putObject(
        instance: T,
        funnel: Funnels.Funnel<T>,
    ): AbstractHasher {
        funnel.funnel(instance, this)
        return this
    }

    abstract fun hash(): HashCode

    companion object {
        private fun checkPositionIndexes(
            start: Int,
            end: Int,
            size: Int,
        ) {
            if (start < 0 || end < start || end > size) {
                throw IndexOutOfBoundsException("start: $start, end: $end, size: $size")
            }
        }
    }
}
