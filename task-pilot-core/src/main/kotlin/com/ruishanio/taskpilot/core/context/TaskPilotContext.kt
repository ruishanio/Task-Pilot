package com.ruishanio.taskpilot.core.context

/**
 * 任务执行上下文。
 *
 * 通过 InheritableThreadLocal 透传到任务创建的子线程，保持日志、分片和处理结果信息可读。
 */
class TaskPilotContext(
    val jobId: Long,
    val jobParam: String?,
    val logId: Long,
    val logDateTime: Long,
    val logFileName: String?,
    val shardIndex: Int,
    val shardTotal: Int
) {
    /**
     * 默认以成功态进入执行流程，仅在任务显式失败或超时时再覆盖。
     */
    var handleCode: Int = HANDLE_CODE_SUCCESS

    /**
     * 执行结果摘要日志。
     */
    var handleMsg: String? = null

    companion object {
        const val HANDLE_CODE_SUCCESS: Int = 200
        const val HANDLE_CODE_FAIL: Int = 500
        const val HANDLE_CODE_TIMEOUT: Int = 502

        /**
         * 支持任务处理器子线程透传上下文。
         */
        private val contextHolder = InheritableThreadLocal<TaskPilotContext>()

        /**
         * 设置当前线程上下文。
         */
        fun setTaskPilotContext(taskPilotContext: TaskPilotContext?) {
            contextHolder.set(taskPilotContext)
        }

        /**
         * 获取当前线程上下文。
         */
        fun getTaskPilotContext(): TaskPilotContext? = contextHolder.get()
    }
}
