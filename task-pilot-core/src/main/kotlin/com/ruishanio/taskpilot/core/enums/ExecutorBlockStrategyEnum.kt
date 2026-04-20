package com.ruishanio.taskpilot.core.enums

/**
 * 执行器阻塞策略枚举。
 * 统一收口到 core/enums 下，便于与 admin 模块中的执行策略实现区分。
 */
enum class ExecutorBlockStrategyEnum(
    /**
     * 展示文案。
     */
    val title: String
) {
    SERIAL_EXECUTION("单机串行"),
    DISCARD_LATER("丢弃后续调度"),
    COVER_EARLY("覆盖之前调度");

    companion object {
        /**
         * 按名称匹配阻塞策略，未命中时回落到默认值。
         */
        fun match(name: String?, defaultItem: ExecutorBlockStrategyEnum?): ExecutorBlockStrategyEnum? {
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
