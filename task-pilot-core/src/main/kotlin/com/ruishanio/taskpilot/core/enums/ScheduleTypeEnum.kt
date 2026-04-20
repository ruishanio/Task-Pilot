package com.ruishanio.taskpilot.core.enums

/**
 * 调度类型枚举。
 * 统一收口到 core/enums 下，避免共享协议枚举散落在不同模块。
 */
enum class ScheduleTypeEnum(
    /**
     * 前端展示文案。
     */
    val title: String
) {
    /**
     * 占位类型，表示任务不会自动计算下次触发时间。
     */
    NONE("无"),
    /**
     * 通过 Cron 表达式描述触发时间。
     */
    CRON("CRON"),
    /**
     * 通过固定秒数间隔触发。
     */
    FIX_RATE("固定速度");

    companion object {
        /**
         * 按名称匹配调度类型，未命中时回落到默认值。
         */
        fun match(name: String?, defaultItem: ScheduleTypeEnum?): ScheduleTypeEnum? {
            if (name != null) {
                for (item in entries) {
                    if (item.name == name) {
                        return item
                    }
                }
            }
            return defaultItem
        }
    }
}
