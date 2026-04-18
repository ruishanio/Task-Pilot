package com.ruishanio.taskpilot.admin.scheduler.misfire.strategy

import com.ruishanio.taskpilot.admin.scheduler.misfire.MisfireHandler
import org.slf4j.LoggerFactory

/**
 * 忽略当前失火窗口，不做补偿触发。
 */
class MisfireDoNothing : MisfireHandler() {
    override fun handle(jobId: Int) {
        logger.warn(">>>>>>>>>>> task-pilot 调度失火策略：忽略本次触发，jobId={}", jobId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MisfireDoNothing::class.java)
    }
}
