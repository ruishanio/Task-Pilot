package com.ruishanio.taskpilot.admin.scheduler.alarm.impl

import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.model.TaskPilotLog
import com.ruishanio.taskpilot.admin.scheduler.alarm.JobAlarm
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.util.I18nUtil
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
class EmailJobAlarm : JobAlarm {
    override fun doAlarm(info: TaskPilotInfo, jobLog: TaskPilotLog): Boolean {
        var alarmResult = true
        if (info.alarmEmail != null && info.alarmEmail!!.trim().isNotEmpty()) {
            var alarmContent = "Alarm Job LogId=${jobLog.id}"
            if (jobLog.triggerCode != TaskPilotContext.HANDLE_CODE_SUCCESS) {
                alarmContent += "<br>TriggerMsg=<br>${jobLog.triggerMsg}"
            }
            if (jobLog.handleCode > 0 && jobLog.handleCode != TaskPilotContext.HANDLE_CODE_SUCCESS) {
                alarmContent += "<br>HandleCode=${jobLog.handleMsg}"
            }

            val group = TaskPilotAdminBootstrap.instance.taskPilotGroupMapper.load(info.jobGroup)
            val personal = I18nUtil.getString("admin_name_full")
            val title = I18nUtil.getString("jobconf_monitor")
            val content = MessageFormat.format(
                loadEmailJobAlarmTemplate(),
                group?.title ?: "null",
                info.id,
                info.jobDesc,
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
                    logger.error(">>>>>>>>>>> task-pilot 发送失败任务告警邮件时发生异常，jobLogId={}, email={}", jobLog.id, email, e)
                    alarmResult = false
                }
            }
        }
        return alarmResult
    }

    /**
     * 模板保持内联字符串形式，避免引入新的模板文件影响已有部署包结构。
     */
    private fun loadEmailJobAlarmTemplate(): String =
        "<h5>${I18nUtil.getString("jobconf_monitor_detail")}：</span>" +
            "<table border=\"1\" cellpadding=\"3\" style=\"border-collapse:collapse; width:80%;\" >\n" +
            "   <thead style=\"font-weight: bold;color: #ffffff;background-color: #ff8c00;\" >" +
            "      <tr>\n" +
            "         <td width=\"20%\" >${I18nUtil.getString("jobinfo_field_jobgroup")}</td>\n" +
            "         <td width=\"10%\" >${I18nUtil.getString("jobinfo_field_id")}</td>\n" +
            "         <td width=\"20%\" >${I18nUtil.getString("jobinfo_field_jobdesc")}</td>\n" +
            "         <td width=\"10%\" >${I18nUtil.getString("jobconf_monitor_alarm_title")}</td>\n" +
            "         <td width=\"40%\" >${I18nUtil.getString("jobconf_monitor_alarm_content")}</td>\n" +
            "      </tr>\n" +
            "   </thead>\n" +
            "   <tbody>\n" +
            "      <tr>\n" +
            "         <td>{0}</td>\n" +
            "         <td>{1}</td>\n" +
            "         <td>{2}</td>\n" +
            "         <td>${I18nUtil.getString("jobconf_monitor_alarm_type")}</td>\n" +
            "         <td>{3}</td>\n" +
            "      </tr>\n" +
            "   </tbody>\n" +
            "</table>"

    companion object {
        private val logger = LoggerFactory.getLogger(EmailJobAlarm::class.java)
    }
}
