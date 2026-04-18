package com.ruishanio.taskpilot.tool.io

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList

/**
 * CSV 工具。
 * 保留轻量级字符串解析实现，避免迁移时引入第三方 CSV 依赖改变转义细节。
 */
object CsvTool {
    private const val DEFAULT_SEPARATOR = ','
    private const val DEFAULT_QUOTE = '"'

    /**
     * 逐字符解析一行 CSV，继续只处理分隔符、双引号转义和换行三类核心场景。
     */
    private fun parseLine(line: String, separator: Char, quote: Char): Array<String> {
        val result = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == quote) {
                if (inQuotes) {
                    if (i + 1 < line.length && line[i + 1] == quote) {
                        field.append(quote)
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    inQuotes = true
                }
            } else if (c == separator && !inQuotes) {
                result.add(field.toString())
                field.setLength(0)
            } else {
                field.append(c)
            }
            i++
        }

        result.add(field.toString())
        return result.toTypedArray()
    }

    /**
     * 输出时只在必要场景补引号，保持历史文件格式尽量紧凑。
     */
    private fun formatLine(values: Array<String>, separator: Char, quote: Char): String {
        val line = StringBuilder()

        for (i in values.indices) {
            val value = values[i]
            val needQuotes =
                value.contains(separator) ||
                    value.contains(quote) ||
                    value.contains("\n")

            if (needQuotes) {
                line.append(quote)
                    .append(value.replace(quote.toString(), quote.toString() + quote))
                    .append(quote)
            } else {
                line.append(value)
            }

            if (i < values.size - 1) {
                line.append(separator)
            }
        }

        return line.toString()
    }

    /** 写 CSV 文件，使用默认分隔符和引号。 */
    fun writeCsv(filePath: String, data: List<Array<String>>) {
        writeCsv(filePath, data, DEFAULT_SEPARATOR, DEFAULT_QUOTE)
    }

    /** 写 CSV 文件，并允许调用方自定义分隔符和引号。 */
    fun writeCsv(filePath: String, data: List<Array<String>>, separator: Char, quote: Char) {
        try {
            BufferedWriter(FileWriter(filePath)).use { bw ->
                for (row in data) {
                    bw.write(formatLine(row, separator, quote))
                    bw.newLine()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("CsvTool writeCsv error.", e)
        }
    }

    /** 读 CSV 文件，使用默认分隔符和引号。 */
    fun readCsv(filePath: String): List<Array<String>> = readCsv(filePath, DEFAULT_SEPARATOR, DEFAULT_QUOTE)

    /** 读 CSV 文件，并按历史逻辑逐行解析。 */
    fun readCsv(filePath: String, separator: Char, quote: Char): List<Array<String>> {
        val result = ArrayList<Array<String>>()

        try {
            BufferedReader(FileReader(filePath)).use { br ->
                var line = br.readLine()
                while (line != null) {
                    result.add(parseLine(line, separator, quote))
                    line = br.readLine()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("CsvTool readCsv error.", e)
        }

        return result
    }
}
