package com.ruishanio.taskpilot.core.constant

/**
 * 执行器阻塞策略枚举。
 *
 * `title` 保持可变，便于启动阶段按国际化资源覆盖默认展示文案。
 */
enum class ExecutorBlockStrategyEnum(
    /**
     * 展示文案。
     */
    var title: String
) {
    SERIAL_EXECUTION("Serial execution"),
    DISCARD_LATER("Discard Later"),
    COVER_EARLY("Cover Early");

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
