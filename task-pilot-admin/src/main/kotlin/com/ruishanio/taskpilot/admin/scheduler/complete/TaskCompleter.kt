package com.ruishanio.taskpilot.admin.scheduler.complete

import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskLogMapper
import com.ruishanio.taskpilot.admin.model.TaskLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.MessageFormat

/**
 * 任务完成处理器。
 *
 * 完成回调时会串联子任务触发与日志裁剪，确保最终入库的 handleMsg 不超过数据库可承受范围。
 */
@Component
class TaskCompleter {
    @Resource
    private lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    private lateinit var taskLogMapper: TaskLogMapper

    /**
     * 任务完成只允许处理一次，先补齐子任务触发信息，再写回 handle 结果。
     */
    fun complete(taskLog: TaskLog): Int {
        processChildJob(taskLog)
        if ((taskLog.handleMsg?.length ?: 0) > 15000) {
            taskLog.handleMsg = taskLog.handleMsg?.substring(0, 15000)
        }
        return taskLogMapper.updateHandleInfo(taskLog)
    }

    /**
     * 成功任务会按声明顺序触发子任务，并把每个子任务的处理结果附加到当前日志。
     */
    private fun processChildJob(taskLog: TaskLog) {
        var triggerChildMsg: String? = null
        if (TaskPilotContext.HANDLE_CODE_SUCCESS == taskLog.handleCode) {
            val taskInfo = taskInfoMapper.loadById(taskLog.taskId)
            if (taskInfo != null && StringTool.isNotBlank(taskInfo.childTaskId)) {
                triggerChildMsg =
                    "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发子任务<<<<<<<<<<< </span><br>"
                val childTaskIds = taskInfo.childTaskId!!.split(",")
                for (i in childTaskIds.indices) {
                    val childTaskId = if (StringTool.isNotBlank(childTaskIds[i]) && StringTool.isNumeric(childTaskIds[i])) {
                        childTaskIds[i].toInt()
                    } else {
                        -1
                    }

                    if (childTaskId > 0) {
                        if (childTaskId == taskLog.taskId) {
                            logger.debug(">>>>>>>>>>> task-pilot 忽略子任务触发，childTaskId={} 与当前任务相同。", childTaskId)
                            continue
                        }

                        TaskPilotAdminBootstrap.instance.taskTriggerPoolHelper.trigger(
                            childTaskId,
                            TriggerTypeEnum.PARENT,
                            -1,
                            null,
                            null,
                            null
                        )
                        val triggerChildResult = Response.ofSuccess<String>()
                        triggerChildMsg += MessageFormat.format(
                            "{0}/{1} [任务ID={2}], 触发{3}, 触发备注: {4} <br>",
                            i + 1,
                            childTaskIds.size,
                            childTaskIds[i],
                            if (triggerChildResult.isSuccess) "成功" else "失败",
                            triggerChildResult.msg
                        )
                    } else {
                        triggerChildMsg += MessageFormat.format(
                            "{0}/{1} [任务ID={2}], 触发失败, 触发备注: 任务ID格式错误 <br>",
                            i + 1,
                            childTaskIds.size,
                            childTaskIds[i]
                        )
                    }
                }
            }
        }

        if (StringTool.isNotBlank(triggerChildMsg)) {
            taskLog.handleMsg = taskLog.handleMsg + triggerChildMsg
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskCompleter::class.java)
    }
}
