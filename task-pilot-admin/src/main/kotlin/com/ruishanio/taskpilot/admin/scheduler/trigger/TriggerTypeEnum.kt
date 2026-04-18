package com.ruishanio.taskpilot.admin.scheduler.trigger

/**
 * 任务触发来源枚举。
 */
enum class TriggerTypeEnum(val title: String) {
    MANUAL("手动触发"),
    CRON("Cron触发"),
    RETRY("失败重试触发"),
    PARENT("父任务触发"),
    API("API触发"),
    MISFIRE("调度过期补偿")
}
