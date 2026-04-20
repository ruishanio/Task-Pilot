package com.ruishanio.taskpilot.admin.scheduler.misfire

import com.ruishanio.taskpilot.admin.scheduler.misfire.strategy.MisfireDoNothing
import com.ruishanio.taskpilot.admin.scheduler.misfire.strategy.MisfireFireOnceNow
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum

/**
 * 把共享失火策略枚举映射到管理端补偿处理器，保证注解、接口和调度线程共用同一套配置值。
 */
fun MisfireStrategyEnum.toMisfireHandler(): MisfireHandler =
    when (this) {
        MisfireStrategyEnum.DO_NOTHING -> MISFIRE_DO_NOTHING
        MisfireStrategyEnum.FIRE_ONCE_NOW -> MISFIRE_FIRE_ONCE_NOW
    }

/**
 * 失火处理器本身无状态，这里集中复用实例，避免调度线程频繁分配对象。
 */
private val MISFIRE_DO_NOTHING = MisfireDoNothing()
private val MISFIRE_FIRE_ONCE_NOW = MisfireFireOnceNow()
