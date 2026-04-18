package com.ruishanio.taskpilot.tool.test.io

import com.ruishanio.taskpilot.tool.io.FileTool
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.slf4j.LoggerFactory

/**
 * 覆盖 FileTool 常见文件生命周期操作。
 * 这里改成运行期临时目录，避免 Kotlin 迁移后继续依赖本地固定路径。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileToolTest {
    private lateinit var tempDir: Path
    private lateinit var testDirPath: String
    private lateinit var testFilePath: String
    private val logger = LoggerFactory.getLogger(FileToolTest::class.java)

    @BeforeEach
    fun beforeEach() {
        FileTool.clean(tempDir.toFile())
        FileTool.delete(testDirPath)
        FileTool.delete(testFilePath)
    }

    @Test
    @DisplayName("测试创建文件对象")
    fun testFile() {
        val file1 = FileTool.file("test.txt")
        assertNotNull(file1)
        assertEquals("test.txt", file1.name)

        val file2 = FileTool.file("parent", "child")
        assertNotNull(file2)
        assertEquals("child", file2.name)
    }

    @Test
    @DisplayName("测试创建实际文件")
    @Throws(IOException::class)
    fun testCreateFile() {
        val file = FileTool.createFile(testFilePath)
        assertTrue(file!!.exists())
        assertTrue(file.isFile)
    }

    @Test
    @DisplayName("测试创建带父路径的实际文件")
    @Throws(IOException::class)
    fun testCreateFileWithParent() {
        val filePath = tempDir.resolve("testDir/subdir/test.txt").toString()
        val file = FileTool.createFile(filePath)
        assertTrue(file!!.exists())
        assertTrue(file.isFile)
    }

    @Test
    @DisplayName("测试创建目录")
    @Throws(IOException::class)
    fun testCreateDirectories() {
        val result = FileTool.createDirectories(File(testDirPath))
        assertTrue(result)
        assertTrue(FileTool.exists(testDirPath))
        assertTrue(FileTool.isDirectory(testDirPath))
    }

    @Test
    @DisplayName("测试创建父目录")
    @Throws(IOException::class)
    fun testCreateParentDirectories() {
        val filePath = tempDir.resolve("testDir/subdir/test.txt").toString()
        val result = FileTool.createParentDirectories(File(filePath))
        assertTrue(result)
        assertTrue(FileTool.exists(testDirPath))
        assertTrue(FileTool.isDirectory(testDirPath))
    }

    @Test
    @DisplayName("测试判断目录")
    @Throws(IOException::class)
    fun testIsDirectory() {
        FileTool.createDirectories(File(testDirPath))
        assertTrue(FileTool.isDirectory(testDirPath))
        assertFalse(FileTool.isDirectory(testFilePath))
    }

    @Test
    @DisplayName("测试判断文件")
    @Throws(IOException::class)
    fun testIsFile() {
        FileTool.createFile(testFilePath)
        assertTrue(FileTool.isFile(testFilePath))
        assertFalse(FileTool.isFile(testDirPath))
    }

    @Test
    @DisplayName("测试判断空文件/目录")
    @Throws(IOException::class)
    fun testIsEmpty() {
        FileTool.createFile(testFilePath)
        assertTrue(FileTool.isEmpty(File(testFilePath)))

        FileTool.createDirectories(File(testDirPath))
        assertTrue(FileTool.isEmpty(File(testDirPath)))

        FileTool.writeString(testFilePath, "test content")
        assertFalse(FileTool.isEmpty(File(testFilePath)))
    }

    @Test
    @DisplayName("测试判断是否为同一文件")
    @Throws(IOException::class)
    fun testIsSameFile() {
        val file1 = FileTool.createFile(testFilePath)
        val file2 = File(testFilePath)
        assertTrue(FileTool.isSameFile(file1, file2))
    }

    @Test
    @DisplayName("测试文件是否存在")
    @Throws(IOException::class)
    fun testExists() {
        assertFalse(FileTool.exists(testFilePath))
        FileTool.createFile(testFilePath)
        assertTrue(FileTool.exists(testFilePath))
    }

    @Test
    @DisplayName("测试获取文件大小")
    @Throws(IOException::class)
    fun testSize() {
        FileTool.createFile(testFilePath)
        assertEquals(0, FileTool.size(testFilePath))

        FileTool.writeString(testFilePath, "test")
        assertEquals(4, FileTool.size(testFilePath))
    }

    @Test
    @DisplayName("测试获取文件行数")
    @Throws(IOException::class)
    fun testTotalLines() {
        FileTool.createFile(testFilePath)
        FileTool.writeString(testFilePath, "line1\nline2\nline3")
        assertEquals(3, FileTool.totalLines(testFilePath))
    }

    @Test
    @DisplayName("测试删除文件")
    @Throws(IOException::class)
    fun testDelete() {
        FileTool.createFile(testFilePath)
        assertTrue(FileTool.exists(testFilePath))
        assertTrue(FileTool.delete(testFilePath))
        assertFalse(FileTool.exists(testFilePath))
    }

    @Test
    @DisplayName("测试清空目录")
    @Throws(IOException::class)
    fun testClean() {
        FileTool.createDirectories(File(testDirPath))
        val filePath = tempDir.resolve("testDir/test.txt").toString()
        FileTool.createFile(filePath)
        assertTrue(FileTool.exists(filePath))

        assertTrue(FileTool.clean(testDirPath))
        assertFalse(FileTool.exists(filePath))
        assertTrue(FileTool.exists(testDirPath))
    }

    @Test
    @DisplayName("测试复制文件")
    @Throws(IOException::class)
    fun testCopy() {
        FileTool.createFile(testFilePath)
        FileTool.writeString(testFilePath, "test content")

        val destPath = tempDir.resolve("copiedFile.txt").toString()
        val destFile = FileTool.copy(testFilePath, destPath, false)

        assertTrue(destFile.exists())
        assertEquals("test content", FileTool.readString(destPath))
    }

    @Test
    @DisplayName("测试移动文件")
    @Throws(IOException::class)
    fun testMove() {
        FileTool.createFile(testFilePath)
        FileTool.writeString(testFilePath, "test content")

        val destPath = tempDir.resolve("movedFile.txt").toString()
        val destFile = FileTool.move(testFilePath, destPath, false)

        assertFalse(FileTool.exists(testFilePath))
        assertTrue(destFile.exists())
        assertEquals("test content", FileTool.readString(destPath))
    }

    @Test
    @DisplayName("测试重命名文件")
    @Throws(IOException::class)
    fun testRename() {
        FileTool.createFile(testFilePath)
        FileTool.writeString(testFilePath, "test content")

        val newFile = FileTool.rename(File(testFilePath), "renamedFile.txt", false)

        assertFalse(FileTool.exists(testFilePath))
        assertTrue(newFile.exists())
        assertEquals("test content", FileTool.readString(newFile.path))
    }

    @Test
    @DisplayName("测试写入字符串")
    @Throws(IOException::class)
    fun testWriteString() {
        FileTool.writeString(testFilePath, "test content")
        assertTrue(FileTool.exists(testFilePath))
        assertEquals("test content", FileTool.readString(testFilePath))

        FileTool.writeString(testFilePath, " append", true)
        assertEquals("test content append", FileTool.readString(testFilePath))
    }

    @Test
    @DisplayName("测试写入多行数据")
    @Throws(IOException::class)
    fun testWriteLines() {
        val lines = listOf("line1", "line2", "line3")
        FileTool.writeLines(testFilePath, lines)
        assertTrue(FileTool.exists(testFilePath))

        val readLines = FileTool.readLines(testFilePath)
        assertEquals(lines, readLines)
    }

    @Test
    @DisplayName("测试使用Supplier写入多行数据")
    @Throws(IOException::class)
    fun testWriteLinesWithSupplier() {
        val data = listOf("line1", "line2", "line3", null)
        val index = AtomicInteger(0)
        val supplier =
            Supplier {
                val i = index.getAndIncrement()
                if (i < data.size) data[i] else null
            }

        FileTool.writeLines(testFilePath, supplier)
        assertTrue(FileTool.exists(testFilePath))

        val readLines = FileTool.readLines(testFilePath)
        assertEquals(listOf("line1", "line2", "line3"), readLines)
    }

    @Test
    @DisplayName("测试读取字符串")
    @Throws(IOException::class)
    fun testReadString() {
        FileTool.createFile(testFilePath)
        FileTool.writeString(testFilePath, "test content")

        val content = FileTool.readString(testFilePath)
        assertEquals("test content", content)
    }

    @Test
    @DisplayName("测试读取多行数据")
    @Throws(IOException::class)
    fun testReadLines() {
        val lines = listOf("line1", "line2", "line3")
        FileTool.writeLines(testFilePath, lines)

        val readLines = FileTool.readLines(testFilePath)
        assertEquals(lines, readLines)
    }

    @Test
    @DisplayName("测试使用Consumer读取多行数据")
    @Throws(IOException::class)
    fun testReadLinesWithConsumer() {
        val lines = listOf("line1", "line2", "line3")
        FileTool.writeLines(testFilePath, lines)

        val readLines = mutableListOf<String>()
        FileTool.readLines(testFilePath, Consumer { readLines.add(it) })

        assertEquals(lines, readLines)
    }

    @Test
    @Throws(IOException::class)
    fun readLinesTest() {
        FileTool.writeLines(testFilePath, listOf("1", "2", "3"))
        FileTool.readLines(
            testFilePath,
            Consumer { line ->
                logger.info(line)
            }
        )
    }

    @BeforeAll
    fun setUp() {
        tempDir = Files.createTempDirectory("task-pilot-filetool-test")
        testDirPath = tempDir.resolve("testDir").toString()
        testFilePath = tempDir.resolve("testFile.txt").toString()
    }

    @AfterAll
    fun tearDown() {
        FileTool.delete(tempDir.toFile())
    }
}
