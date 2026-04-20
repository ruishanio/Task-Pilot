package com.ruishanio.taskpilot.admin.scheduler.misfire.strategy

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.misfire.MisfireHandler
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import org.slf4j.LoggerFactory

/**
 * 立即补触发一次失火任务。
 */
class MisfireFireOnceNow : MisfireHandler() {
    override fun handle(taskId: Int) {
        TaskPilotAdminBootstrap.instance.taskTriggerPoolHelper.trigger(
            taskId,
            TriggerTypeEnum.MISFIRE,
            -1,
            null,
            null,
            null
        )
        logger.warn(">>>>>>>>>>> task-pilot 调度失火策略：立即补触发一次，taskId={}", taskId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MisfireFireOnceNow::class.java)
    }
}
