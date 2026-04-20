package com.ruishanio.taskpilot.admin.scheduler.alarm

import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.model.TaskLog
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.MapTool
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * 告警聚合器。
 *
 * 所有告警器都执行完成才返回成功，避免部分渠道失败被静默吞掉。
 */
@Component
class JobAlarmer : ApplicationContextAware, InitializingBean {
    private lateinit var applicationContext: ApplicationContext
    private var jobAlarmList: List<JobAlarm>? = null

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        val serviceBeanMap = applicationContext.getBeansOfType(JobAlarm::class.java)
        if (MapTool.isNotEmpty(serviceBeanMap)) {
            jobAlarmList = ArrayList(serviceBeanMap.values)
        }
    }

    /**
     * 依次执行全部告警处理器，任何一个失败都会把最终结果标记为失败。
     */
    fun alarm(info: TaskInfo, jobLog: TaskLog): Boolean {
        var result = false
        if (CollectionTool.isNotEmpty(jobAlarmList)) {
            result = true
            for (alarm in jobAlarmList!!) {
                val resultItem = try {
                    alarm.doAlarm(info, jobLog)
                } catch (e: Exception) {
                    logger.error("执行告警处理器时发生异常。alarm={}", alarm.javaClass.simpleName, e)
                    false
                }
                if (!resultItem) {
                    result = false
                }
            }
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobAlarmer::class.java)
    }
}
