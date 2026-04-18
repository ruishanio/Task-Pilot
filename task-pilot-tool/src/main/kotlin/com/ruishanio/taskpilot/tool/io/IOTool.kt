package com.ruishanio.taskpilot.tool.io

import com.ruishanio.taskpilot.tool.core.AssertTool
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.channels.Channels
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * IO 工具。
 * 继续提供最基础的流复制、读取和 writer 构建能力，避免下游工具类重复实现相同模板代码。
 */
object IOTool {
    const val BUFFER_SIZE: Int = 1024 * 8

    /** 复制输入流到输出流，默认在完成后关闭两端流。 */
    @Throws(IOException::class)
    fun copy(input: InputStream?, output: OutputStream?): Int = copy(input, output, true, true)

    /**
     * 复制输入流到输出流，并允许显式控制是否关闭两端流。
     */
    @Throws(IOException::class)
    fun copy(input: InputStream?, output: OutputStream?, closeInput: Boolean, closeOutput: Boolean): Int {
        AssertTool.notNull(input, "No InputStream specified")
        AssertTool.notNull(output, "No OutputStream specified")
        val actualInput = input!!
        val actualOutput = output!!

        try {
            var byteCount = 0
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead = actualInput.read(buffer)
            while (bytesRead != -1) {
                actualOutput.write(buffer, 0, bytesRead)
                byteCount += bytesRead
                bytesRead = actualInput.read(buffer)
            }
            actualOutput.flush()
            return byteCount
        } finally {
            if (closeInput) {
                close(actualInput)
            }
            if (closeOutput) {
                close(actualOutput)
            }
        }
    }

    /** 将字节数组写入输出流，默认在完成后关闭输出流。 */
    @Throws(IOException::class)
    fun copy(input: ByteArray?, output: OutputStream?) {
        copy(input, output, true)
    }

    /** 将字节数组写入输出流，并允许控制输出流关闭策略。 */
    @Throws(IOException::class)
    fun copy(input: ByteArray?, output: OutputStream?, closeOutput: Boolean) {
        AssertTool.notNull(input, "No input byte array specified")
        AssertTool.notNull(output, "No OutputStream specified")
        val actualOutput = output!!

        try {
            actualOutput.write(input)
        } finally {
            if (closeOutput) {
                close(actualOutput)
            }
        }
    }

    /** 关闭资源，忽略关闭过程中的异常。 */
    fun close(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (_: IOException) {
        }
    }

    /** 读取输入流全部字节，按历史行为在结束后关闭输入流。 */
    @Throws(IOException::class)
    fun readBytes(inputStream: InputStream?): ByteArray {
        if (inputStream == null) {
            return ByteArray(0)
        }

        val out = ByteArrayOutputStream(BUFFER_SIZE)
        copy(inputStream, out)
        return out.toByteArray()
    }

    /** 使用 UTF-8 读取输入流字符串。 */
    @Throws(IOException::class)
    fun readString(inputStream: InputStream?): String = readString(inputStream, StandardCharsets.UTF_8)

    /** 读取输入流字符串，并按历史行为同时关闭输入流和 reader。 */
    @Throws(IOException::class)
    fun readString(inputStream: InputStream?, charset: Charset?): String {
        if (inputStream == null) {
            return ""
        }

        val actualCharset = charset ?: StandardCharsets.UTF_8
        val out = StringBuilder(BUFFER_SIZE)
        var reader: InputStreamReader? = null
        try {
            reader = InputStreamReader(inputStream, actualCharset)
            val buffer = CharArray(BUFFER_SIZE)
            var charsRead = reader.read(buffer)
            while (charsRead != -1) {
                out.append(buffer, 0, charsRead)
                charsRead = reader.read(buffer)
            }
        } finally {
            close(inputStream)
            close(reader)
        }
        return out.toString()
    }

    /** 按 UTF-8 将字符串写入输出流。 */
    @Throws(IOException::class)
    fun writeString(data: String?, output: OutputStream) {
        writeString(data, output, StandardCharsets.UTF_8)
    }

    /** 将字符串按指定字符集写入输出流。 */
    @Throws(IOException::class)
    fun writeString(data: String?, output: OutputStream, charset: Charset?) {
        if (data == null) {
            return
        }
        val actualCharset = charset ?: StandardCharsets.UTF_8
        Channels.newChannel(output).write(actualCharset.encode(data))
    }

    /** 使用默认 UTF-8 与默认缓冲区创建 writer。 */
    @Throws(IOException::class)
    fun newBufferedWriter(file: File, append: Boolean): BufferedWriter {
        return newBufferedWriter(file, append, StandardCharsets.UTF_8, BUFFER_SIZE)
    }

    /** 创建带编码与缓冲区配置的 writer。 */
    @Throws(IOException::class)
    fun newBufferedWriter(file: File, append: Boolean, charset: Charset, bufferSize: Int): BufferedWriter {
        return BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(file, append),
                charset
            ),
            bufferSize
        )
    }
}
