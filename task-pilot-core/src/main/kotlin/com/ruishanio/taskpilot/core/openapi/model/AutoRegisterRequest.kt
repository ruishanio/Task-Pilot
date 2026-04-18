package com.ruishanio.taskpilot.core.openapi.model

/**
 * 执行器与任务自动注册请求。
 */
data class AutoRegisterRequest(
    /**
     * 执行器 AppName。
     */
    var appname: String? = null,
    /**
     * 执行器分组标题。
     */
    var title: String? = null,
    /**
     * 需要自动新增或同步更新的任务列表。
     */
    var tasks: MutableList<Task> = mutableListOf()
) {
    /**
     * 单个任务的自动注册声明。
     */
    data class Task(
        var executorHandler: String? = null,
        var jobDesc: String? = null,
        var author: String? = null,
        var alarmEmail: String? = null,
        var scheduleType: String? = null,
        var scheduleConf: String? = null,
        var misfireStrategy: String? = null,
        var executorRouteStrategy: String? = null,
        var executorParam: String? = null,
        var executorBlockStrategy: String? = null,
        var executorTimeout: Int = 0,
        var executorFailRetryCount: Int = 0,
        var childJobId: String? = null,
        var autoStart: Boolean = false
    )
}
