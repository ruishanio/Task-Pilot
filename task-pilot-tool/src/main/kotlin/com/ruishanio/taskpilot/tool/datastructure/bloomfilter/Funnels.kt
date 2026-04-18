package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import java.io.Serializable
import java.nio.charset.StandardCharsets

/**
 * Funnel 集合。
 * 继续把对象转字节的逻辑做成可复用常量，避免 BloomFilter 调用侧重复写样板转换。
 */
object Funnels {
    fun interface Funnel<in T : Any> : Serializable {
        fun funnel(
            from: T,
            into: AbstractHasher,
        )
    }
    val STRING: Funnel<CharSequence> =
        Funnel { from, into ->
            into.putString(from, StandardCharsets.UTF_8)
        }
    val UNENCODED_CHARS: Funnel<CharSequence> =
        Funnel { from, into ->
            into.putUnencodedChars(from)
        }
    val BYTE_ARRAY: Funnel<ByteArray> =
        Funnel { from, into ->
            into.putBytes(from)
        }
    val INTEGER: Funnel<Int> =
        Funnel { from, into ->
            into.putInt(from)
        }
    val LONG: Funnel<Long> =
        Funnel { from, into ->
            into.putLong(from)
        }
}
