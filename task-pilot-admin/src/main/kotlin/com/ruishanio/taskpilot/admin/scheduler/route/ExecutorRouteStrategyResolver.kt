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
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum

/**
 * 把共享路由策略枚举适配为管理端路由器实现，避免策略名称在多个模块重复定义。
 */
fun ExecutorRouteStrategyEnum.toRouter(): ExecutorRouter? =
    when (this) {
        ExecutorRouteStrategyEnum.FIRST -> EXECUTOR_ROUTE_FIRST
        ExecutorRouteStrategyEnum.LAST -> EXECUTOR_ROUTE_LAST
        ExecutorRouteStrategyEnum.ROUND -> EXECUTOR_ROUTE_ROUND
        ExecutorRouteStrategyEnum.RANDOM -> EXECUTOR_ROUTE_RANDOM
        ExecutorRouteStrategyEnum.CONSISTENT_HASH -> EXECUTOR_ROUTE_CONSISTENT_HASH
        ExecutorRouteStrategyEnum.LEAST_FREQUENTLY_USED -> EXECUTOR_ROUTE_LFU
        ExecutorRouteStrategyEnum.LEAST_RECENTLY_USED -> EXECUTOR_ROUTE_LRU
        ExecutorRouteStrategyEnum.FAILOVER -> EXECUTOR_ROUTE_FAILOVER
        ExecutorRouteStrategyEnum.BUSYOVER -> EXECUTOR_ROUTE_BUSYOVER
        ExecutorRouteStrategyEnum.SHARDING_BROADCAST -> null
    }

/**
 * 路由器实现无状态，集中复用可以减少每次触发时的对象创建开销。
 */
private val EXECUTOR_ROUTE_FIRST = ExecutorRouteFirst()
private val EXECUTOR_ROUTE_LAST = ExecutorRouteLast()
private val EXECUTOR_ROUTE_ROUND = ExecutorRouteRound()
private val EXECUTOR_ROUTE_RANDOM = ExecutorRouteRandom()
private val EXECUTOR_ROUTE_CONSISTENT_HASH = ExecutorRouteConsistentHash()
private val EXECUTOR_ROUTE_LFU = ExecutorRouteLFU()
private val EXECUTOR_ROUTE_LRU = ExecutorRouteLRU()
private val EXECUTOR_ROUTE_FAILOVER = ExecutorRouteFailover()
private val EXECUTOR_ROUTE_BUSYOVER = ExecutorRouteBusyover()
