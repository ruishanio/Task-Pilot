package com.ruishanio.taskpilot.tool.io

import com.ruishanio.taskpilot.tool.core.ArrayTool
import com.ruishanio.taskpilot.tool.core.AssertTool
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.AccessDeniedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * 文件工具。
 * 继续保留 `String/File/Path` 三套入口，减少现有模块在 Kotlin 迁移过程中被迫统一路径类型。
 */
object FileTool {
    private const val DEFAULT_LINE_BUFFER_SIZE: Int = 1024
    private const val CR_BYTE: Byte = '\r'.code.toByte()
    private const val LF_BYTE: Byte = '\n'.code.toByte()
    fun file(path: String): File = File(path)
    fun file(parent: String, child: String): File = File(parent, child)
    @Throws(IOException::class)
    fun createFile(path: String): File? = createFile(file(path))
    @Throws(IOException::class)
    fun createFile(parent: String, child: String): File? = createFile(file(parent, child))

    /**
     * 创建文件时继续遵循“已存在直接返回”的策略，避免历史调用链额外处理并发创建冲突。
     */
    @Throws(IOException::class)
    fun createFile(file: File?): File? {
        if (file == null) {
            return null
        }
        if (exists(file)) {
            return file
        }

        createParentDirectories(file)
        try {
            Files.createFile(file.toPath())
        } catch (_: FileAlreadyExistsException) {
            return file
        }
        return file
    }
    @Throws(IOException::class)
    fun createParentDirectories(file: File?): Boolean {
        if (file == null) {
            return false
        }
        return createDirectories(file.parentFile)
    }
    @Throws(IOException::class)
    fun createDirectories(dir: File?): Boolean {
        if (dir == null) {
            return false
        }

        if (exists(dir)) {
            if (isDirectory(dir)) {
                return true
            }
            throw RuntimeException("file is not directory, path=${dir.path}")
        }

        Files.createDirectories(dir.toPath())
        return true
    }
    fun isDirectory(path: String?): Boolean = path != null && Files.isDirectory(Paths.get(path))
    fun isDirectory(file: File?): Boolean = file != null && Files.isDirectory(file.toPath())
    fun isFile(path: String?): Boolean = path != null && file(path).isFile
    fun isFile(file: File?): Boolean = file != null && file.isFile
    fun isEmpty(file: File?): Boolean {
        if (!exists(file)) {
            return true
        }

        return when {
            file!!.isDirectory -> ArrayTool.isEmpty(file.list())
            file.isFile -> file.length() <= 0
            else -> false
        }
    }
    fun isNotEmpty(file: File?): Boolean = !isEmpty(file)
    @Throws(IOException::class)
    fun isSameFile(file1: File?, file2: File?): Boolean {
        if (file1 == null || file2 == null) {
            return false
        }
        return Files.isSameFile(file1.toPath(), file2.toPath())
    }
    fun isSub(parent: Path, sub: Path): Boolean = toAbsoluteNormal(sub).startsWith(toAbsoluteNormal(parent))
    fun exists(path: String?): Boolean = path != null && exists(file(path))
    fun exists(file: File?): Boolean = file != null && Files.exists(file.toPath())
    fun toAbsoluteNormal(path: Path): Path {
        AssertTool.notNull(path, "path is null")
        return path.toAbsolutePath().normalize()
    }
    fun toAbsolutePath(path: Path): Path {
        AssertTool.notNull(path, "path is null")
        return path.toAbsolutePath()
    }
    fun size(path: String): Long = size(file(path), false)
    fun size(file: File): Long = size(file, false)

    /**
     * 目录大小继续递归累加，并默认跳过符号链接，避免扫描时把链接目标重复算入。
     */
    fun size(file: File?, includeDirSize: Boolean): Long {
        if (!exists(file) || Files.isSymbolicLink(file!!.toPath())) {
            return 0
        }

        if (file.isDirectory) {
            var size = if (includeDirSize) file.length() else 0
            val subFiles = file.listFiles()
            if (ArrayTool.isEmpty(subFiles)) {
                return 0L
            }
            for (subFile in subFiles!!) {
                size += size(subFile, includeDirSize)
            }
            return size
        }
        return file.length()
    }
    fun totalLines(path: String): Int = totalLines(file(path), DEFAULT_LINE_BUFFER_SIZE, true)
    fun totalLines(file: File): Int = totalLines(file, DEFAULT_LINE_BUFFER_SIZE, true)

    /**
     * 行数统计继续按字节扫描处理 CR/LF，保持对大文件的低内存占用和旧结果一致。
     */
    fun totalLines(file: File?, bufferSize: Int, lastLineSeparatorAsLine: Boolean): Int {
        if (!isFile(file)) {
            throw RuntimeException("file invalid")
        }
        val actualBufferSize = if (bufferSize < 1) DEFAULT_LINE_BUFFER_SIZE else bufferSize

        BufferedInputStream(FileInputStream(file)).use { inputStream ->
            val buffer = ByteArray(actualBufferSize)
            var bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) {
                return 0
            }

            var lineCount = 1
            var previousChar: Byte = 0
            var currentChar: Byte = 0

            while (bytesRead != -1) {
                for (i in 0 until bytesRead) {
                    previousChar = currentChar
                    currentChar = buffer[i]
                    if (currentChar == LF_BYTE || previousChar == CR_BYTE) {
                        lineCount++
                    }
                }
                bytesRead = inputStream.read(buffer)
            }

            if (lastLineSeparatorAsLine) {
                if (currentChar == CR_BYTE) {
                    lineCount++
                }
            } else {
                if (currentChar == LF_BYTE) {
                    lineCount--
                }
            }
            return lineCount
        }
    }
    fun delete(path: String): Boolean = delete(file(path))
    fun delete(file: File?): Boolean {
        if (!exists(file)) {
            return true
        }

        if (file!!.isDirectory) {
            val cleanResult = clean(file)
            if (!cleanResult) {
                return false
            }
        }

        val path = file.toPath()
        try {
            Files.delete(path)
        } catch (_: AccessDeniedException) {
            return file.delete()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return true
    }
    fun clean(dirPath: String): Boolean = clean(file(dirPath), false)
    fun clean(directory: File?): Boolean = clean(directory, false)
    fun clean(directory: File?, skipDeleteFailFile: Boolean): Boolean {
        if (!exists(directory)) {
            return true
        }
        if (!isDirectory(directory)) {
            return false
        }

        var result = true
        val files = directory!!.listFiles()
        if (ArrayTool.isNotEmpty(files)) {
            for (childFile in files!!) {
                val deleteResult = delete(childFile)
                if (!deleteResult) {
                    result = false
                    if (!skipDeleteFailFile) {
                        return false
                    }
                }
            }
        }
        return result
    }
    @Throws(IOException::class)
    fun copy(src: String, dest: String, isOverride: Boolean): File {
        AssertTool.notBlank(src, "source file path is blank")
        AssertTool.notBlank(dest, "destination file path is blank")
        return copy(Paths.get(src), Paths.get(dest), isOverride)
    }
    @Throws(IOException::class)
    fun copy(src: File, dest: File, isOverride: Boolean): File {
        AssertTool.notNull(src, "source file is null")
        AssertTool.notNull(dest, "destination file is null")
        return copy(src.toPath(), dest.toPath(), isOverride)
    }
    @Throws(IOException::class)
    fun copy(src: Path, dest: Path, isOverride: Boolean): File {
        AssertTool.notNull(src, "source file is null")
        AssertTool.notNull(dest, "destination file is null")
        val copyOptions = if (isOverride) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
        return copy(src, dest, *copyOptions)
    }

    /**
     * copy 继续只支持文件，不额外扩展目录复制，避免和 move/delete 的递归策略耦合。
     */
    @Throws(IOException::class)
    fun copy(src: Path, dest: Path, vararg options: StandardCopyOption): File {
        AssertTool.notNull(src, "source file path is null")
        AssertTool.notNull(dest, "destination file file is null")

        val srcFile = src.toFile()
        val destFile = dest.toFile()
        if (!exists(srcFile)) {
            throw RuntimeException("source file not exists")
        }
        if (!isFile(srcFile)) {
            throw RuntimeException("source only support file")
        }
        if (toAbsoluteNormal(srcFile.toPath()) == toAbsoluteNormal(destFile.toPath())) {
            throw RuntimeException("destination file can not be same as source file")
        }
        if (isSub(src, dest)) {
            throw RuntimeException("destination can not be sub of source")
        }

        val finalDestFilePath = if (isDirectory(destFile)) dest.resolve(src.fileName) else dest
        createParentDirectories(destFile)
        return Files.copy(src, finalDestFilePath, *options).toFile()
    }
    @Throws(IOException::class)
    fun move(src: String, dest: String, isOverride: Boolean): File {
        AssertTool.notBlank(src, "source file path is blank")
        AssertTool.notBlank(dest, "destination file path is blank")
        return move(Paths.get(src), Paths.get(dest), isOverride)
    }
    @Throws(IOException::class)
    fun move(src: File, dest: File, isOverride: Boolean): File {
        AssertTool.notNull(src, "source file is null")
        AssertTool.notNull(dest, "destination file is null")
        return move(src.toPath(), dest.toPath(), isOverride)
    }
    @Throws(IOException::class)
    fun move(src: Path, dest: Path, isOverride: Boolean): File {
        AssertTool.notNull(src, "source file is null")
        AssertTool.notNull(dest, "destination file is null")
        val copyOptions = if (isOverride) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
        return move(src, dest, *copyOptions)
    }
    @Throws(IOException::class)
    fun move(src: Path, dest: Path, vararg options: StandardCopyOption): File {
        AssertTool.notNull(src, "source file path is null")
        AssertTool.notNull(dest, "destination file file is null")

        val srcFile = src.toFile()
        val destFile = dest.toFile()
        if (!exists(srcFile)) {
            throw RuntimeException("source file not exists")
        }
        if (toAbsoluteNormal(srcFile.toPath()) == toAbsoluteNormal(destFile.toPath())) {
            throw RuntimeException("destination file can not be same as source file")
        }
        if (isSub(src, dest)) {
            throw RuntimeException("destination can not be sub of source")
        }

        val finalDestFilePath = if (isDirectory(destFile)) dest.resolve(src.fileName) else dest
        createParentDirectories(destFile)
        return Files.move(src, finalDestFilePath, *options).toFile()
    }
    @Throws(IOException::class)
    fun rename(file: File, newFileName: String, isOverride: Boolean): File {
        AssertTool.notNull(file, "file path is null")
        AssertTool.notBlank(newFileName, "newFileName is blank")
        return move(file.toPath(), file.toPath().resolveSibling(newFileName), isOverride)
    }
    @Throws(IOException::class)
    fun writeString(path: String, content: String?) {
        writeString(path, content, false, StandardCharsets.UTF_8)
    }
    @Throws(IOException::class)
    fun writeString(path: String, content: String?, append: Boolean) {
        writeString(path, content, append, StandardCharsets.UTF_8)
    }
    @Throws(IOException::class)
    fun writeString(path: String, content: String?, append: Boolean, charset: Charset?) {
        AssertTool.notBlank(path, "file path is null")
        if (content == null) {
            return
        }
        val actualCharset = charset ?: StandardCharsets.UTF_8
        val file = prepareWritableFile(path)

        var writer: BufferedWriter? = null
        try {
            writer = IOTool.newBufferedWriter(file, append, actualCharset, IOTool.BUFFER_SIZE)
            writer.write(content)
            writer.flush()
        } finally {
            IOTool.close(writer)
        }
    }
    @Throws(IOException::class)
    fun writeLines(path: String, lines: Iterable<*>?) {
        writeLines(path, lines, null, true, null)
    }
    @Throws(IOException::class)
    fun writeLines(path: String, lines: Iterable<*>?, append: Boolean) {
        writeLines(path, lines, null, append, null)
    }
    @Throws(IOException::class)
    fun writeLines(
        path: String,
        lines: Iterable<*>?,
        lineSeparator: String?,
        append: Boolean,
        charset: Charset?,
    ) {
        AssertTool.notBlank(path, "file path is null")
        if (lines == null || !lines.iterator().hasNext()) {
            return
        }
        val actualLineSeparator = lineSeparator ?: System.lineSeparator()
        val actualCharset = charset ?: StandardCharsets.UTF_8
        val file = prepareWritableFile(path)

        var writer: BufferedWriter? = null
        try {
            writer = IOTool.newBufferedWriter(file, append, actualCharset, IOTool.BUFFER_SIZE)
            for (line in lines) {
                if (line != null) {
                    writer.write(line.toString())
                }
                writer.write(actualLineSeparator)
            }
            writer.flush()
        } finally {
            IOTool.close(writer)
        }
    }
    @Throws(IOException::class)
    fun writeLines(path: String, lineSupplier: Supplier<*>?) {
        writeLines(path, lineSupplier, null, true, null)
    }
    @Throws(IOException::class)
    fun writeLines(path: String, lineSupplier: Supplier<*>?, append: Boolean) {
        writeLines(path, lineSupplier, null, append, null)
    }
    @Throws(IOException::class)
    fun writeLines(
        path: String,
        lineSupplier: Supplier<*>?,
        lineSeparator: String?,
        append: Boolean,
        charset: Charset?,
    ) {
        AssertTool.notBlank(path, "file path is null")
        AssertTool.notNull(lineSupplier, "supplier is null")
        val actualLineSeparator = lineSeparator ?: System.lineSeparator()
        val actualCharset = charset ?: StandardCharsets.UTF_8
        val file = prepareWritableFile(path)

        var writer: BufferedWriter? = null
        try {
            writer = IOTool.newBufferedWriter(file, append, actualCharset, IOTool.BUFFER_SIZE)
            var line = lineSupplier!!.get()
            while (line != null) {
                writer.write(line.toString())
                writer.write(actualLineSeparator)
                line = lineSupplier.get()
            }
            writer.flush()
        } finally {
            IOTool.close(writer)
        }
    }
    @Throws(IOException::class)
    fun readString(path: String): String = readString(path, StandardCharsets.UTF_8)
    @Throws(IOException::class)
    fun readString(path: String, charset: Charset?): String {
        AssertTool.notBlank(path, "file path is null")
        val actualCharset = charset ?: StandardCharsets.UTF_8
        return Files.readString(Paths.get(path), actualCharset)
    }
    @Throws(IOException::class)
    fun readLines(path: String): List<String> = readLines(path, StandardCharsets.UTF_8)
    @Throws(IOException::class)
    fun readLines(path: String, charset: Charset?): List<String> {
        AssertTool.notBlank(path, "file path is null")
        val actualCharset = charset ?: StandardCharsets.UTF_8
        return Files.readAllLines(Paths.get(path), actualCharset)
    }
    @Throws(IOException::class)
    fun readLines(path: String, lineConsumer: Consumer<String>) {
        readLines(path, StandardCharsets.UTF_8, lineConsumer)
    }
    @Throws(IOException::class)
    fun readLines(path: String, charset: Charset?, lineConsumer: Consumer<String>) {
        AssertTool.notBlank(path, "file path is null")
        AssertTool.notNull(lineConsumer, "lineConsumer is null")
        val actualCharset = charset ?: StandardCharsets.UTF_8

        Files.newBufferedReader(Paths.get(path), actualCharset).use { reader ->
            consumeLines(reader, lineConsumer)
        }
    }

    /**
     * 写入前继续统一校验“路径存在但不是文件”的情况，避免 append/override 行为默默落到目录上。
     */
    @Throws(IOException::class)
    private fun prepareWritableFile(path: String): File {
        val file = file(path)
        if (!exists(path)) {
            createFile(file)
        } else if (!isFile(file)) {
            throw RuntimeException("path($path) is not a file")
        }
        return file
    }

    private fun consumeLines(
        reader: BufferedReader,
        lineConsumer: Consumer<String>,
    ) {
        var line = reader.readLine()
        while (line != null) {
            lineConsumer.accept(line)
            line = reader.readLine()
        }
    }
}
