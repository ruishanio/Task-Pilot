package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import java.util.ArrayList
import java.util.HashMap
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 最不经常使用路由。
 *
 * 计数按任务维度维护，并在地址集变化时同步剔除失效节点，避免旧地址持续参与比较。
 */
class ExecutorRouteLFU : ExecutorRouter() {
    fun route(jobId: Int, addressList: List<String>): String {
        if (System.currentTimeMillis() > cacheValidTime) {
            jobLfuMap.clear()
            cacheValidTime = System.currentTimeMillis() + 1000L * 60 * 60 * 24
        }

        val lfuItemMap = jobLfuMap.computeIfAbsent(jobId) { HashMap() }

        for (address in addressList) {
            if (!lfuItemMap.containsKey(address) || (lfuItemMap[address] ?: 0) > 1_000_000) {
                lfuItemMap[address] = Random().nextInt(addressList.size)
            }
        }

        val removeKeys = ArrayList<String>()
        for (existKey in lfuItemMap.keys) {
            if (!addressList.contains(existKey)) {
                removeKeys.add(existKey)
            }
        }
        for (removeKey in removeKeys) {
            lfuItemMap.remove(removeKey)
        }

        val addressItem = lfuItemMap.entries.sortedBy { it.value }.first()
        addressItem.setValue(addressItem.value + 1)
        return addressItem.key
    }

    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> =
        Response.ofSuccess(route(triggerParam.jobId, addressList))

    companion object {
        private val jobLfuMap: ConcurrentMap<Int, HashMap<String, Int>> = ConcurrentHashMap()
        private var cacheValidTime: Long = 0
    }
}
