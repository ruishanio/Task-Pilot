package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 基于任务维度的轮询路由。
 *
 * 继续保留按天清空计数缓存的策略，避免长期运行时计数无限增长。
 */
class ExecutorRouteRound : ExecutorRouter() {
    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> {
        val address = addressList[count(triggerParam.taskId) % addressList.size]
        return Response.ofSuccess(address)
    }

    companion object {
        private val routeCountEachTask: ConcurrentMap<Int, AtomicInteger> = ConcurrentHashMap()
        private var cacheValidTime: Long = 0

        /**
         * 为每个任务维护独立轮询游标，首次访问时先随机打散，减轻冷启动热点。
         */
        private fun count(taskId: Int): Int {
            if (System.currentTimeMillis() > cacheValidTime) {
                routeCountEachTask.clear()
                cacheValidTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24
            }

            val counter = routeCountEachTask.compute(taskId) { _, current ->
                when {
                    current == null || current.get() > 1_000_000 -> AtomicInteger(Random().nextInt(100))
                    else -> {
                        current.incrementAndGet()
                        current
                    }
                }
            } ?: AtomicInteger(Random().nextInt(100))

            return counter.get()
        }
    }
}
