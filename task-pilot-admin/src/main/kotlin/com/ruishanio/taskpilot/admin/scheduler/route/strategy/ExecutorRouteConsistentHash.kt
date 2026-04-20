package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.SortedMap
import java.util.TreeMap

/**
 * 一致性哈希路由。
 *
 * 使用虚拟节点平衡分布，保证同一任务在地址集稳定时尽量落到固定执行器。
 */
class ExecutorRouteConsistentHash : ExecutorRouter() {
    fun hashTask(taskId: Int, addressList: List<String>): String {
        val addressRing = TreeMap<Long, String>()
        for (address in addressList) {
            for (i in 0 until VIRTUAL_NODE_NUM) {
                val addressHash = hash("SHARD-$address-NODE-$i")
                addressRing[addressHash] = address
            }
        }

        val taskHash = hash(taskId.toString())
        val tailRing: SortedMap<Long, String> = addressRing.tailMap(taskHash)
        return if (tailRing.isNotEmpty()) {
            tailRing[tailRing.firstKey()]!!
        } else {
            addressRing.firstEntry().value
        }
    }

    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> =
        Response.ofSuccess(hashTask(triggerParam.taskId, addressList))

    companion object {
        private const val VIRTUAL_NODE_NUM = 100

        /**
         * 使用 MD5 扩展 hash 取值范围，避免直接依赖字符串 hashCode 的碰撞风险。
         */
        private fun hash(key: String): Long {
            val md5 = try {
                MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("MD5 not supported", e)
            }

            md5.reset()
            md5.update(key.toByteArray(StandardCharsets.UTF_8))
            val digest = md5.digest()

            val hashCode = ((digest[3].toLong() and 0xFF) shl 24) or
                ((digest[2].toLong() and 0xFF) shl 16) or
                ((digest[1].toLong() and 0xFF) shl 8) or
                (digest[0].toLong() and 0xFF)

            return hashCode and 0xffffffffL
        }
    }
}
