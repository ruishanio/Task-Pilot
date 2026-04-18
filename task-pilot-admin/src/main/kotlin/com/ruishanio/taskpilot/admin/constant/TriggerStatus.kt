package com.ruishanio.taskpilot.admin.constant

/**
 * 任务触发状态枚举。
 *
 * 保持 value/desc 可变，兼容历史代码对枚举实例进行文案覆写的访问方式。
 */
enum class TriggerStatus(
    var value: Int,
    var desc: String
) {
    STOPPED(0, "stopped"),
    RUNNING(1, "running")
}
