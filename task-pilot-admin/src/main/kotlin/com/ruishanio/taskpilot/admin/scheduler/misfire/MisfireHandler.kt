package com.ruishanio.taskpilot.admin.scheduler.misfire

/**
 * 调度失火处理器抽象。
 */
abstract class MisfireHandler {
    /**
     * 处理指定任务的失火补偿逻辑。
     */
    abstract fun handle(jobId: Int)
}
