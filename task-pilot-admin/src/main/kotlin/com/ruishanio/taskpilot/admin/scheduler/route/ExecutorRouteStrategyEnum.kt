package com.ruishanio.taskpilot.admin.scheduler.route

import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteBusyover
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteConsistentHash
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteFailover
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteFirst
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteLFU
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteLRU
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteLast
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteRandom
import com.ruishanio.taskpilot.admin.scheduler.route.strategy.ExecutorRouteRound

/**
 * 执行器路由策略枚举。
 *
 * 路由名称与处理器实例在枚举内集中维护，避免调度配置解析时分散判断。
 */
enum class ExecutorRouteStrategyEnum(
    val title: String,
    val router: ExecutorRouter?
) {
    FIRST("第一个", ExecutorRouteFirst()),
    LAST("最后一个", ExecutorRouteLast()),
    ROUND("轮询", ExecutorRouteRound()),
    RANDOM("随机", ExecutorRouteRandom()),
    CONSISTENT_HASH("一致性HASH", ExecutorRouteConsistentHash()),
    LEAST_FREQUENTLY_USED("最不经常使用", ExecutorRouteLFU()),
    LEAST_RECENTLY_USED("最近最久未使用", ExecutorRouteLRU()),
    FAILOVER("故障转移", ExecutorRouteFailover()),
    BUSYOVER("忙碌转移", ExecutorRouteBusyover()),
    SHARDING_BROADCAST("分片广播", null);

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
