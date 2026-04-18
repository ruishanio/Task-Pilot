package com.ruishanio.taskpilot.admin.scheduler.complete

import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotLogMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import com.ruishanio.taskpilot.admin.util.I18nUtil
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
class JobCompleter {
    @Resource
    private lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Resource
    private lateinit var taskPilotLogMapper: TaskPilotLogMapper

    /**
     * 任务完成只允许处理一次，先补齐子任务触发信息，再写回 handle 结果。
     */
    fun complete(taskPilotLog: TaskPilotLog): Int {
        processChildJob(taskPilotLog)
        if ((taskPilotLog.handleMsg?.length ?: 0) > 15000) {
            taskPilotLog.handleMsg = taskPilotLog.handleMsg?.substring(0, 15000)
        }
        return taskPilotLogMapper.updateHandleInfo(taskPilotLog)
    }

    /**
     * 成功任务会按声明顺序触发子任务，并把每个子任务的处理结果附加到当前日志。
     */
    private fun processChildJob(taskPilotLog: TaskPilotLog) {
        var triggerChildMsg: String? = null
        if (TaskPilotContext.HANDLE_CODE_SUCCESS == taskPilotLog.handleCode) {
            val taskPilotInfo = taskPilotInfoMapper.loadById(taskPilotLog.jobId)
            if (taskPilotInfo != null && StringTool.isNotBlank(taskPilotInfo.childJobId)) {
                triggerChildMsg =
                    "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>${I18nUtil.getString("jobconf_trigger_child_run")}<<<<<<<<<<< </span><br>"
                val childJobIds = taskPilotInfo.childJobId!!.split(",")
                for (i in childJobIds.indices) {
                    val childJobId = if (StringTool.isNotBlank(childJobIds[i]) && StringTool.isNumeric(childJobIds[i])) {
                        childJobIds[i].toInt()
                    } else {
                        -1
                    }

                    if (childJobId > 0) {
                        if (childJobId == taskPilotLog.jobId) {
                            logger.debug(">>>>>>>>>>> task-pilot 忽略子任务触发，childJobId={} 与当前任务相同。", childJobId)
                            continue
                        }

                        TaskPilotAdminBootstrap.instance.jobTriggerPoolHelper.trigger(
                            childJobId,
                            TriggerTypeEnum.PARENT,
                            -1,
                            null,
                            null,
                            null
                        )
                        val triggerChildResult = Response.ofSuccess<String>()
                        triggerChildMsg += MessageFormat.format(
                            I18nUtil.getString("jobconf_callback_child_msg1"),
                            i + 1,
                            childJobIds.size,
                            childJobIds[i],
                            if (triggerChildResult.isSuccess) I18nUtil.getString("system_success") else I18nUtil.getString("system_fail"),
                            triggerChildResult.msg
                        )
                    } else {
                        triggerChildMsg += MessageFormat.format(
                            I18nUtil.getString("jobconf_callback_child_msg2"),
                            i + 1,
                            childJobIds.size,
                            childJobIds[i]
                        )
                    }
                }
            }
        }

        if (StringTool.isNotBlank(triggerChildMsg)) {
            taskPilotLog.handleMsg = taskPilotLog.handleMsg + triggerChildMsg
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobCompleter::class.java)
    }
}
