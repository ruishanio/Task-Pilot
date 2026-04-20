package com.ruishanio.taskpilot.core.enums

/**
 * 执行器路由策略枚举。
 * 统一收口到 core/enums 下，保证调度协议与实现适配层之间的命名稳定。
 */
enum class ExecutorRouteStrategyEnum(
    /**
     * 前端展示文案。
     */
    val title: String
) {
    /**
     * 固定使用第一个可用执行器。
     */
    FIRST("第一个"),
    /**
     * 固定使用最后一个可用执行器。
     */
    LAST("最后一个"),
    /**
     * 按任务维度轮询分配执行器。
     */
    ROUND("轮询"),
    /**
     * 从候选执行器中随机选择。
     */
    RANDOM("随机"),
    /**
     * 基于一致性哈希选择执行器。
     */
    CONSISTENT_HASH("一致性HASH"),
    /**
     * 优先选择调用频率最低的执行器。
     */
    LEAST_FREQUENTLY_USED("最不经常使用"),
    /**
     * 优先选择最近最久未使用的执行器。
     */
    LEAST_RECENTLY_USED("最近最久未使用"),
    /**
     * 按探活结果做故障转移。
     */
    FAILOVER("故障转移"),
    /**
     * 按执行器空闲状态做忙碌转移。
     */
    BUSYOVER("忙碌转移"),
    /**
     * 对所有在线执行器做广播触发。
     */
    SHARDING_BROADCAST("分片广播");

    companion object {
        /**
         * 按名称匹配路由策略，未命中时回落到默认值。
         */
        fun match(name: String?, defaultItem: ExecutorRouteStrategyEnum?): ExecutorRouteStrategyEnum? {
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
