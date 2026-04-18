package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Murmur3 128 位哈希器。
 * 继续按小端序处理字节，保证和旧版 Java 实现生成完全一致的哈希结果。
 */
class Murmur3_128Hasher(
    seed: Int,
) : StreamingHasher(CHUNK_SIZE) {
    private var h1: Long = seed.toLong()
    private var h2: Long = seed.toLong()
    private var length: Int = 0

    override fun process(byteBuffer: ByteBuffer) {
        val k1 = byteBuffer.getLong()
        val k2 = byteBuffer.getLong()
        bmix64(k1, k2)
        length += CHUNK_SIZE
    }

    override fun processRemaining(byteBuffer: ByteBuffer) {
        var k1 = 0L
        var k2 = 0L
        val remaining = byteBuffer.remaining()
        length += remaining

        val duplicate = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        val k1Size = kotlin.math.min(8, remaining)
        for (i in 0 until k1Size) {
            k1 = k1 xor ((duplicate.get(i).toInt() and 0xFF).toLong() shl (i * 8))
        }
        for (i in 8 until remaining) {
            k2 = k2 xor ((duplicate.get(i).toInt() and 0xFF).toLong() shl ((i - 8) * 8))
        }

        h1 = h1 xor mixK1(k1)
        h2 = h2 xor mixK2(k2)
    }

    override fun makeHash(): HashCode {
        h1 = h1 xor length.toLong()
        h2 = h2 xor length.toLong()

        h1 += h2
        h2 += h1

        h1 = fmix64(h1)
        h2 = fmix64(h2)

        h1 += h2
        h2 += h1

        return HashCode.fromBytesNoCopy(
            ByteBuffer
                .wrap(ByteArray(CHUNK_SIZE))
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(h1)
                .putLong(h2)
                .array(),
        )
    }

    private fun bmix64(
        k1: Long,
        k2: Long,
    ) {
        h1 = h1 xor mixK1(k1)
        h1 = java.lang.Long.rotateLeft(h1, 27)
        h1 += h2
        h1 = h1 * 5 + 0x52dce729

        h2 = h2 xor mixK2(k2)
        h2 = java.lang.Long.rotateLeft(h2, 31)
        h2 += h1
        h2 = h2 * 5 + 0x38495ab5
    }

    companion object {
        private const val CHUNK_SIZE: Int = 16
        private const val C1: Long = -8663945395140668459L
        private const val C2: Long = 5545529020109919103L

        private fun mixK1(k1: Long): Long {
            var finalK1 = k1 * C1
            finalK1 = java.lang.Long.rotateLeft(finalK1, 31)
            finalK1 *= C2
            return finalK1
        }

        private fun mixK2(k2: Long): Long {
            var finalK2 = k2 * C2
            finalK2 = java.lang.Long.rotateLeft(finalK2, 33)
            finalK2 *= C1
            return finalK2
        }

        private fun fmix64(k: Long): Long {
            var finalK = k
            finalK = finalK xor (finalK ushr 33)
            finalK *= -49064778989728563L
            finalK = finalK xor (finalK ushr 33)
            finalK *= -4265267296055464877L
            finalK = finalK xor (finalK ushr 33)
            return finalK
        }
    }
}
