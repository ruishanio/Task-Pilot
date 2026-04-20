package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 最近最少使用路由。
 *
 * 继续依赖 access-order 的 `LinkedHashMap` 维护访问顺序，保持与旧实现一致的淘汰语义。
 */
class ExecutorRouteLRU : ExecutorRouter() {
    fun route(taskId: Int, addressList: List<String>): String {
        if (System.currentTimeMillis() > cacheValidTime) {
            taskLruMap.clear()
            cacheValidTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24
        }

        val lruItem = taskLruMap.computeIfAbsent(taskId) {
            LinkedHashMap<String, String>(16, 0.75f, true)
        }

        for (address in addressList) {
            if (!lruItem.containsKey(address)) {
                lruItem[address] = address
            }
        }

        val removeKeys = ArrayList<String>()
        for (existKey in lruItem.keys) {
            if (!addressList.contains(existKey)) {
                removeKeys.add(existKey)
            }
        }
        for (removeKey in removeKeys) {
            lruItem.remove(removeKey)
        }

        val eldestKey = lruItem.entries.iterator().next().key
        return lruItem[eldestKey]!!
    }

    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> =
        Response.ofSuccess(route(triggerParam.taskId, addressList))

    companion object {
        private val taskLruMap: ConcurrentMap<Int, LinkedHashMap<String, String>> = ConcurrentHashMap()
        private var cacheValidTime: Long = 0
    }
}
