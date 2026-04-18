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
import com.ruishanio.taskpilot.admin.util.I18nUtil

/**
 * 执行器路由策略枚举。
 *
 * 路由名称与处理器实例在枚举内集中维护，避免调度配置解析时分散判断。
 */
enum class ExecutorRouteStrategyEnum(
    val title: String,
    val router: ExecutorRouter?
) {
    FIRST(I18nUtil.getString("jobconf_route_first"), ExecutorRouteFirst()),
    LAST(I18nUtil.getString("jobconf_route_last"), ExecutorRouteLast()),
    ROUND(I18nUtil.getString("jobconf_route_round"), ExecutorRouteRound()),
    RANDOM(I18nUtil.getString("jobconf_route_random"), ExecutorRouteRandom()),
    CONSISTENT_HASH(I18nUtil.getString("jobconf_route_consistenthash"), ExecutorRouteConsistentHash()),
    LEAST_FREQUENTLY_USED(I18nUtil.getString("jobconf_route_lfu"), ExecutorRouteLFU()),
    LEAST_RECENTLY_USED(I18nUtil.getString("jobconf_route_lru"), ExecutorRouteLRU()),
    FAILOVER(I18nUtil.getString("jobconf_route_failover"), ExecutorRouteFailover()),
    BUSYOVER(I18nUtil.getString("jobconf_route_busyover"), ExecutorRouteBusyover()),
    SHARDING_BROADCAST(I18nUtil.getString("jobconf_route_shard"), null);

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
