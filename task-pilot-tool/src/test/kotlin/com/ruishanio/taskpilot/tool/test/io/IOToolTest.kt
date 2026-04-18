package com.ruishanio.taskpilot.tool.test.io

import com.ruishanio.taskpilot.tool.io.IOTool
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory

/**
 * 覆盖 IOTool 的基础流式操作，确保 Kotlin 迁移后异常语义不变。
 */
class IOToolTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testCopyWithNormalStreams() {
        val testData = "Hello World".toByteArray(StandardCharsets.UTF_8)
        val input = ByteArrayInputStream(testData)
        val output = ByteArrayOutputStream()

        val copiedBytes = IOTool.copy(input, output)

        assertEquals(testData.size, copiedBytes)
        assertArrayEquals(testData, output.toByteArray())
    }

    @Test
    fun testCopyWithNullInputStream() {
        val output: OutputStream = ByteArrayOutputStream()

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                IOTool.copy(null as InputStream?, output)
            }

        assertEquals("No InputStream specified", exception.message)
    }

    @Test
    fun testCopyWithNullOutputStream() {
        val input: InputStream = ByteArrayInputStream(byteArrayOf(1, 2, 3))

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                IOTool.copy(input, null)
            }

        assertEquals("No OutputStream specified", exception.message)
    }

    @Test
    fun testCopyWithByteArray() {
        val testData = "Test Data".toByteArray(StandardCharsets.UTF_8)
        val output = ByteArrayOutputStream()

        IOTool.copy(testData, output)

        assertArrayEquals(testData, output.toByteArray())
    }

    @Test
    fun testCloseWithValidCloseable() {
        var closed = false
        val closeable =
            Closeable {
                closed = true
            }

        IOTool.close(closeable)

        assertTrue(closed)
    }

    @Test
    fun testCloseWithNullCloseable() {
        assertDoesNotThrow { IOTool.close(null) }
    }

    @Test
    fun testReadBytesWithNormalStream() {
        val testData = "Read Bytes Test".toByteArray(StandardCharsets.UTF_8)
        val input = ByteArrayInputStream(testData)

        val result = IOTool.readBytes(input)

        assertArrayEquals(testData, result)
    }

    @Test
    fun testReadBytesWithNullStream() {
        val result = IOTool.readBytes(null)
        assertEquals(0, result.size)
    }

    @Test
    fun testReadStringWithNormalStream() {
        val testData = "Read String Test"
        val input = ByteArrayInputStream(testData.toByteArray(StandardCharsets.UTF_8))

        val result = IOTool.readString(input, StandardCharsets.UTF_8)

        assertEquals(testData, result)
    }

    @Test
    fun testReadStringWithNullStream() {
        val result = IOTool.readString(null, StandardCharsets.UTF_8)
        assertEquals("", result)
    }

    @Test
    fun testWriteStringWithNormalData() {
        val testData = "Write String Test"
        val output = ByteArrayOutputStream()

        IOTool.writeString(testData, output)

        assertEquals(testData, output.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun testWriteStringWithNullData() {
        val output = ByteArrayOutputStream()
        assertDoesNotThrow { IOTool.writeString(null, output) }
    }

    @Test
    fun testWriteStringWithSpecifiedCharset() {
        val testData = "字符编码测试"
        val output = ByteArrayOutputStream()

        IOTool.writeString(testData, output, StandardCharsets.UTF_8)

        assertEquals(testData, output.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun testNewBufferedWriterWithDefaultParameters() {
        val testFile = tempDir.resolve("test.txt").toFile()
        val writer: BufferedWriter = IOTool.newBufferedWriter(testFile, false)
        assertNotNull(writer)
        writer.close()
    }

    @Test
    fun testNewBufferedWriterWithAllParameters() {
        val testFile: File = tempDir.resolve("test2.txt").toFile()
        val writer = IOTool.newBufferedWriter(testFile, true, StandardCharsets.UTF_8, 512)
        assertNotNull(writer)
        writer.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IOToolTest::class.java)
    }
}
