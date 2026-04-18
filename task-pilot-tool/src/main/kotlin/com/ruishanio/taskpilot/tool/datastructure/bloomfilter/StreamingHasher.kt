package com.ruishanio.taskpilot.tool.datastructure.bloomfilter

import com.ruishanio.taskpilot.tool.core.AssertTool
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 流式哈希器。
 * 使用内部缓冲区批量吃字节，避免调用方逐字节写入时频繁触发具体哈希算法。
 */
abstract class StreamingHasher protected constructor(
    private val chunkSize: Int,
    private val bufferSize: Int = chunkSize,
) : AbstractHasher() {
    private val buffer: ByteBuffer

    init {
        AssertTool.isTrue(bufferSize % chunkSize == 0, "bufferSize must be a multiple of chunkSize")
        buffer = ByteBuffer.allocate(bufferSize + 7).order(ByteOrder.LITTLE_ENDIAN)
    }

    protected abstract fun process(byteBuffer: ByteBuffer)

    override fun putBytes(
        bytes: ByteArray,
        off: Int,
        len: Int,
    ): AbstractHasher = putBytesInternal(ByteBuffer.wrap(bytes, off, len).order(ByteOrder.LITTLE_ENDIAN))

    override fun putBytes(byteBuffer: ByteBuffer): AbstractHasher {
        val order = byteBuffer.order()
        return try {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            putBytesInternal(byteBuffer)
        } finally {
            byteBuffer.order(order)
        }
    }

    override fun putByte(b: Byte): AbstractHasher {
        buffer.put(b)
        munchIfFull()
        return this
    }

    override fun putShort(s: Short): AbstractHasher {
        buffer.putShort(s)
        munchIfFull()
        return this
    }

    override fun putChar(c: Char): AbstractHasher {
        buffer.putChar(c)
        munchIfFull()
        return this
    }

    override fun putInt(i: Int): AbstractHasher {
        buffer.putInt(i)
        munchIfFull()
        return this
    }

    override fun putLong(l: Long): AbstractHasher {
        buffer.putLong(l)
        munchIfFull()
        return this
    }

    override fun hash(): HashCode {
        munch()
        buffer.flip()
        if (buffer.remaining() > 0) {
            processRemaining(buffer)
            buffer.position(buffer.limit())
        }
        return makeHash()
    }

    /**
     * 默认用 0 填充剩余字节，再交给标准块处理逻辑，保持算法实现集中在一处。
     */
    protected open fun processRemaining(byteBuffer: ByteBuffer) {
        byteBuffer.position(byteBuffer.limit())
        byteBuffer.limit(chunkSize + 7)
        while (byteBuffer.position() < chunkSize) {
            byteBuffer.putLong(0)
        }
        byteBuffer.limit(chunkSize)
        byteBuffer.flip()
        process(byteBuffer)
    }

    protected abstract fun makeHash(): HashCode

    private fun putBytesInternal(readBuffer: ByteBuffer): AbstractHasher {
        if (readBuffer.remaining() <= buffer.remaining()) {
            buffer.put(readBuffer)
            munchIfFull()
            return this
        }

        val bytesToCopy = bufferSize - buffer.position()
        for (i in 0 until bytesToCopy) {
            buffer.put(readBuffer.get())
        }
        munch()

        while (readBuffer.remaining() >= chunkSize) {
            process(readBuffer)
        }

        buffer.put(readBuffer)
        return this
    }

    private fun munchIfFull() {
        if (buffer.remaining() < 8) {
            munch()
        }
    }

    private fun munch() {
        buffer.flip()
        while (buffer.remaining() >= chunkSize) {
            process(buffer)
        }
        buffer.compact()
    }
}
