package com.ruishanio.taskpilot.admin.scheduler.type

import com.ruishanio.taskpilot.admin.scheduler.type.strategy.CronScheduleType
import com.ruishanio.taskpilot.admin.scheduler.type.strategy.FixRateScheduleType
import com.ruishanio.taskpilot.admin.scheduler.type.strategy.NoneScheduleType

/**
 * 调度类型枚举。
 *
 * 调度类型与对应实现绑定在枚举内，避免外层通过字符串分派。
 */
enum class ScheduleTypeEnum(
    val title: String,
    val scheduleType: ScheduleType
) {
    NONE("无", NoneScheduleType()),
    CRON("CRON", CronScheduleType()),
    FIX_RATE("固定速度", FixRateScheduleType());

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
