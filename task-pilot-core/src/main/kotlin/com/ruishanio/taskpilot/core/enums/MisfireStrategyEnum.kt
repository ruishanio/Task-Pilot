package com.ruishanio.taskpilot.core.enums

/**
 * 调度失火策略枚举。
 * 统一收口到 core/enums 下，避免不同模块出现同义但不同名的策略类型。
 */
enum class MisfireStrategyEnum(
    /**
     * 前端展示文案。
     */
    val title: String
) {
    /**
     * 跳过当前失火窗口，不做额外补偿。
     */
    DO_NOTHING("忽略"),
    /**
     * 发现失火后立即补触发一次。
     */
    FIRE_ONCE_NOW("立即执行一次");

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
