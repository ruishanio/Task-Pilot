package com.ruishanio.taskpilot.core.util

import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.tool.core.ArrayTool
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.io.IOTool
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

/**
 * 脚本执行工具。
 *
 * 设计取舍：
 * 1、使用外部进程执行脚本，避免嵌入式解释器无法加载扩展包；
 * 2、标准输出和错误输出并发写入日志文件，优先保证可观测性；
 * 3、执行机需自行保证 PATH 中存在对应脚本解释器。
 */
object ScriptUtil {
    /**
     * 生成脚本文件。
     */
    @Throws(IOException::class)
    fun markScriptFile(scriptFileName: String, scriptContent: String?) {
        FileTool.writeString(scriptFileName, scriptContent)
    }

    /**
     * 执行脚本并把日志持续写入目标文件。
     */
    @Throws(IOException::class)
    fun execToFile(command: String, scriptFile: String, logFile: String, vararg params: String): Int {
        var fileOutputStream: FileOutputStream? = null
        var inputThread: Thread? = null
        var errorThread: Thread? = null
        var process: Process? = null

        try {
            fileOutputStream = FileOutputStream(logFile, true)

            val cmdArray = ArrayList<String>()
            cmdArray.add(command)
            cmdArray.add(scriptFile)
            if (ArrayTool.isNotEmpty(params)) {
                for (param in params) {
                    cmdArray.add(param)
                }
            }

            process = Runtime.getRuntime().exec(cmdArray.toTypedArray())
            val finalProcess = process
            val finalFileOutputStream = fileOutputStream

            inputThread =
                thread(start = false) {
                    try {
                        IOTool.copy(finalProcess.inputStream, finalFileOutputStream, true, false)
                    } catch (e: IOException) {
                        TaskPilotHelper.log(e)
                    }
                }
            errorThread =
                thread(start = false) {
                    try {
                        IOTool.copy(finalProcess.errorStream, finalFileOutputStream, true, false)
                    } catch (e: IOException) {
                        TaskPilotHelper.log(e)
                    }
                }

            inputThread.start()
            errorThread.start()

            val exitValue = process.waitFor()
            inputThread.join()
            errorThread.join()
            return exitValue
        } catch (e: Exception) {
            TaskPilotHelper.log(e)
            return -1
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: IOException) {
                    TaskPilotHelper.log(e)
                }
            }
            if (inputThread != null && inputThread.isAlive) {
                inputThread.interrupt()
            }
            if (errorThread != null && errorThread.isAlive) {
                errorThread.interrupt()
            }
            process?.destroy()
        }
    }
}
