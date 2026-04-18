package com.ruishanio.taskpilot.tool.error

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Throwable 文本化工具，统一把完整堆栈转成字符串，便于日志和远程回传场景复用。
 */
object ThrowableTool {
    fun toString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        return stringWriter.toString()
    }
}
