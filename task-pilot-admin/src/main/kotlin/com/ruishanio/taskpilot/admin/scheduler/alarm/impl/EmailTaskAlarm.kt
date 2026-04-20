package com.ruishanio.taskpilot.admin.scheduler.alarm.impl

import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.model.TaskLog
import com.ruishanio.taskpilot.admin.scheduler.alarm.TaskAlarm
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.text.MessageFormat

/**
 * 邮件告警处理器。
 *
 * 收件人列表按逗号拆分后去重，避免同一邮箱因配置重复被发送多次。
 */
@Component
class EmailTaskAlarm : TaskAlarm {
    override fun doAlarm(info: TaskInfo, taskLog: TaskLog): Boolean {
        var alarmResult = true
        if (info.alarmEmail != null && info.alarmEmail!!.trim().isNotEmpty()) {
            var alarmContent = "Alarm Task LogId=${taskLog.id}"
            if (taskLog.triggerCode != TaskPilotContext.HANDLE_CODE_SUCCESS) {
                alarmContent += "<br>TriggerMsg=<br>${taskLog.triggerMsg}"
            }
            if (taskLog.handleCode > 0 && taskLog.handleCode != TaskPilotContext.HANDLE_CODE_SUCCESS) {
                alarmContent += "<br>HandleCode=${taskLog.handleMsg}"
            }

            val group = TaskPilotAdminBootstrap.instance.executorMapper.load(info.executorId)
            val personal = "Task Pilot｜分布式任务调度平台"
            val title = "Task Pilot 监控报警"
            val content = MessageFormat.format(
                loadEmailTaskAlarmTemplate(),
                group?.title ?: "null",
                info.id,
                info.taskDesc,
                alarmContent
            )

            val emailSet = info.alarmEmail!!
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            for (email in emailSet) {
                try {
                    val mimeMessage: MimeMessage = TaskPilotAdminBootstrap.instance.mailSender.createMimeMessage()
                    val helper = MimeMessageHelper(mimeMessage, true)
                    helper.setFrom(TaskPilotAdminBootstrap.instance.emailFrom, personal)
                    helper.setTo(email)
                    helper.setSubject(title)
                    helper.setText(content, true)
                    TaskPilotAdminBootstrap.instance.mailSender.send(mimeMessage)
                } catch (e: Exception) {
                    logger.error(">>>>>>>>>>> task-pilot 发送失败任务告警邮件时发生异常，taskLogId={}, email={}", taskLog.id, email, e)
                    alarmResult = false
                }
            }
        }
        return alarmResult
    }

    /**
     * 模板保持内联字符串形式，避免引入新的模板文件影响已有部署包结构。
     */
    private fun loadEmailTaskAlarmTemplate(): String =
        "<h5>监控告警明细：</span>" +
            "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
            "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
            "      <tr>\n" +
            "         <td width=\"20%\" >执行器</td>\n" +
            "         <td width=\"10%\" >任务ID</td>\n" +
            "         <td width=\"20%\" >任务描述</td>\n" +
            "         <td width=\"10%\" >告警类型</td>\n" +
            "         <td width=\"40%\" >告警内容</td>\n" +
            "      </tr>\n" +
            "   </thead>\n" +
            "   <tbody>\n" +
            "      <tr>\n" +
            "         <td>{0}</td>\n" +
            "         <td>{1}</td>\n" +
            "         <td>{2}</td>\n" +
            "         <td>调度失败</td>\n" +
            "         <td>{3}</td>\n" +
            "      </tr>\n" +
            "   </tbody>\n" +
            "</table>"

    companion object {
        private val logger = LoggerFactory.getLogger(EmailTaskAlarm::class.java)
    }
}
