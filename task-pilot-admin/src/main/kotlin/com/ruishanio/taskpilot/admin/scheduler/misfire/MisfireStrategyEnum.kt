package com.ruishanio.taskpilot.admin.scheduler.misfire

import com.ruishanio.taskpilot.admin.scheduler.misfire.strategy.MisfireDoNothing
import com.ruishanio.taskpilot.admin.scheduler.misfire.strategy.MisfireFireOnceNow
import com.ruishanio.taskpilot.admin.util.I18nUtil

/**
 * 调度失火策略枚举。
 *
 * 通过枚举本身收口策略名称与处理器实例，避免调度层散落字符串分支。
 */
enum class MisfireStrategyEnum(
    val title: String,
    val misfireHandler: MisfireHandler
) {
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing"), MisfireDoNothing()),
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"), MisfireFireOnceNow());

    companion object {
        /**
         * 按名称匹配失火策略，未命中时回落到默认值。
         */
        fun match(name: String?, defaultItem: MisfireStrategyEnum?): MisfireStrategyEnum? {
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
