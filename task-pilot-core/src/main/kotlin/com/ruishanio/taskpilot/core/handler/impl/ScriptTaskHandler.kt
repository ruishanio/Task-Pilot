package com.ruishanio.taskpilot.core.handler.impl

import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.core.handler.ITaskHandler
import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.core.util.ScriptUtil
import java.io.File

/**
 * 脚本型任务处理器。
 *
 * 每次 GLUE 版本变化都会清理旧脚本文件，避免历史脚本残留导致执行来源混淆。
 */
class ScriptTaskHandler(
    private val taskId: Int,
    private val glueUpdateTime: Long,
    private val glueSource: String?,
    private val glueType: GlueTypeEnum?
) : ITaskHandler() {
    init {
        val glueSrcPath = File(TaskPilotFileAppender.getGlueSrcPath())
        if (glueSrcPath.exists()) {
            val glueSrcFileList = glueSrcPath.listFiles()
            if (glueSrcFileList != null) {
                for (glueSrcFileItem in glueSrcFileList) {
                    if (glueSrcFileItem.name.startsWith("${taskId}_")) {
                        glueSrcFileItem.delete()
                    }
                }
            }
        }
    }

    fun getGlueUpdateTime(): Long = glueUpdateTime

    @Throws(Exception::class)
    override fun execute() {
        if (glueType?.isScript != true) {
            TaskPilotHelper.handleFail("glueType[$glueType] 无效。")
            return
        }

        val cmd = glueType.cmd
        val suffix = glueType.suffix
        if (cmd.isNullOrBlank() || suffix.isNullOrBlank()) {
            TaskPilotHelper.handleFail("glueType[$glueType] 缺少脚本命令或后缀配置。")
            return
        }

        val scriptFileName =
                TaskPilotFileAppender.getGlueSrcPath() +
                File.separator +
                taskId +
                "_" +
                glueUpdateTime +
                suffix
        val scriptFile = File(scriptFileName)
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, glueSource)
        }

        val taskPilotContext = TaskPilotContext.getTaskPilotContext()
        val logFileName = taskPilotContext?.logFileName
        if (logFileName.isNullOrBlank()) {
            TaskPilotHelper.handleFail("日志文件未初始化，脚本任务无法执行。")
            return
        }
        val currentTaskPilotContext = requireNotNull(taskPilotContext)

        val taskParam = TaskPilotHelper.getTaskParam() ?: ""
        val shardIndex = currentTaskPilotContext.shardIndex
        val shardTotal = currentTaskPilotContext.shardTotal
        val scriptParams = arrayOf(taskParam, shardIndex.toString(), shardTotal.toString())

        TaskPilotHelper.log("----------- 脚本文件:{} -----------", scriptFileName)
        val exitValue = ScriptUtil.execToFile(cmd, scriptFileName, logFileName, *scriptParams)
        if (exitValue == 0) {
            TaskPilotHelper.handleSuccess()
        } else {
            TaskPilotHelper.handleFail("脚本退出码($exitValue) 表示执行失败。")
        }
    }
}
