package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import java.io.Serializable

/**
 * 哈希策略集合。
 * 继续保留 32/64 两套 Mitz 变体，避免外层 BloomFilter 创建逻辑改动。
 */
object HashStrategies {
    interface Strategy : Serializable {
        fun <T : Any> put(
            `object`: T,
            funnel: Funnels.Funnel<T>,
            numHashFunctions: Int,
            bits: LockFreeBitArray,
        ): Boolean

        fun <T : Any> mightContain(
            `object`: T,
            funnel: Funnels.Funnel<T>,
            numHashFunctions: Int,
            bits: LockFreeBitArray,
        ): Boolean
    }
    val MURMURHASH3_128_MITZ_32: Strategy =
        object : Strategy {
            override fun <T : Any> put(
                `object`: T,
                funnel: Funnels.Funnel<T>,
                numHashFunctions: Int,
                bits: LockFreeBitArray,
            ): Boolean {
                val bitSize = bits.bitSize()
                val hash64 = hashObject(`object`, funnel).asLong()
                val hash1 = hash64.toInt()
                val hash2 = (hash64 ushr 32).toInt()

                var bitsChanged = false
                for (i in 1..numHashFunctions) {
                    var combinedHash = hash1 + (i * hash2)
                    if (combinedHash < 0) {
                        combinedHash = combinedHash.inv()
                    }
                    bitsChanged = bitsChanged or bits.set(combinedHash.toLong() % bitSize)
                }
                return bitsChanged
            }

            override fun <T : Any> mightContain(
                `object`: T,
                funnel: Funnels.Funnel<T>,
                numHashFunctions: Int,
                bits: LockFreeBitArray,
            ): Boolean {
                val bitSize = bits.bitSize()
                val hash64 = hashObject(`object`, funnel).asLong()
                val hash1 = hash64.toInt()
                val hash2 = (hash64 ushr 32).toInt()

                for (i in 1..numHashFunctions) {
                    var combinedHash = hash1 + (i * hash2)
                    if (combinedHash < 0) {
                        combinedHash = combinedHash.inv()
                    }
                    if (!bits.get(combinedHash.toLong() % bitSize)) {
                        return false
                    }
                }
                return true
            }
        }
    val MURMURHASH3_128_MITZ_64: Strategy =
        object : Strategy {
            override fun <T : Any> put(
                `object`: T,
                funnel: Funnels.Funnel<T>,
                numHashFunctions: Int,
                bits: LockFreeBitArray,
            ): Boolean {
                val bitSize = bits.bitSize()
                val bytes = hashObject(`object`, funnel).getBytesInternal()
                val hash1 = lowerEight(bytes)
                val hash2 = upperEight(bytes)

                var bitsChanged = false
                var combinedHash = hash1
                for (i in 0 until numHashFunctions) {
                    bitsChanged = bitsChanged or bits.set((combinedHash and Long.MAX_VALUE) % bitSize)
                    combinedHash += hash2
                }
                return bitsChanged
            }

            override fun <T : Any> mightContain(
                `object`: T,
                funnel: Funnels.Funnel<T>,
                numHashFunctions: Int,
                bits: LockFreeBitArray,
            ): Boolean {
                val bitSize = bits.bitSize()
                val bytes = hashObject(`object`, funnel).getBytesInternal()
                val hash1 = lowerEight(bytes)
                val hash2 = upperEight(bytes)

                var combinedHash = hash1
                for (i in 0 until numHashFunctions) {
                    if (!bits.get((combinedHash and Long.MAX_VALUE) % bitSize)) {
                        return false
                    }
                    combinedHash += hash2
                }
                return true
            }
        }

    private fun <T : Any> hashObject(
        instance: T,
        funnel: Funnels.Funnel<T>,
    ): HashCode {
        val hasher: AbstractHasher = Murmur3_128Hasher(0)
        funnel.funnel(instance, hasher)
        return hasher.hash()
    }

    private fun lowerEight(bytes: ByteArray): Long =
        longFromBytes(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0])

    private fun upperEight(bytes: ByteArray): Long =
        longFromBytes(bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8])

    private fun longFromBytes(
        b1: Byte,
        b2: Byte,
        b3: Byte,
        b4: Byte,
        b5: Byte,
        b6: Byte,
        b7: Byte,
        b8: Byte,
    ): Long =
        ((b1.toLong() and 0xFFL) shl 56) or
            ((b2.toLong() and 0xFFL) shl 48) or
            ((b3.toLong() and 0xFFL) shl 40) or
            ((b4.toLong() and 0xFFL) shl 32) or
            ((b5.toLong() and 0xFFL) shl 24) or
            ((b6.toLong() and 0xFFL) shl 16) or
            ((b7.toLong() and 0xFFL) shl 8) or
            (b8.toLong() and 0xFFL)
}
