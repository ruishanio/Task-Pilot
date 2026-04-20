package com.ruishanio.taskpilot.admin.scheduler.thread

import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.model.Registry
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 执行器注册辅助类。
 *
 * 注册/摘除请求走异步线程池，监控线程则周期性回收失活节点并刷新自动注册分组地址。
 */
class ExecutorRegistryHelper {
    private lateinit var registryOrRemoveThreadPool: ThreadPoolExecutor
    private lateinit var registryMonitorThread: Thread

    @Volatile
    private var toStop: Boolean = false

    fun start() {
        registryOrRemoveThreadPool = ThreadPoolExecutor(
            2,
            10,
            30L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(2000),
            ThreadFactory { runnable ->
                Thread(runnable, "task-pilot, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-${runnable.hashCode()}")
            },
            RejectedExecutionHandler { runnable, _ ->
                runnable.run()
                logger.warn(">>>>>>>>>>> task-pilot 注册或摘除请求过于频繁，线程池已触发拒绝策略，改为调用线程直接执行。")
            }
        )

        registryMonitorThread = Thread {
            while (!toStop) {
                try {
                    val groupList = TaskPilotAdminBootstrap.instance.executorMapper.findByAddressType(0)
                    if (groupList.isNotEmpty()) {
                        val ids = TaskPilotAdminBootstrap.instance.registryMapper.findDead(Const.DEAD_TIMEOUT, Date())
                        if (ids.isNotEmpty()) {
                            TaskPilotAdminBootstrap.instance.registryMapper.removeDead(ids)
                        }

                        val appAddressMap = HashMap<String, MutableList<String>>()
                        val list: List<Registry> =
                            TaskPilotAdminBootstrap.instance.registryMapper.findAll(Const.DEAD_TIMEOUT, Date())
                        for (item in list) {
                            if (RegistType.EXECUTOR.name == item.registryGroup) {
                                val appname = item.registryKey ?: continue
                                val registryList = appAddressMap.getOrPut(appname) { ArrayList() }
                                val registryValue = item.registryValue ?: continue
                                if (!registryList.contains(registryValue)) {
                                    registryList.add(registryValue)
                                }
                            }
                        }

                        for (group in groupList) {
                            val registryList = appAddressMap[group.appname]
                            val addressListStr = if (!registryList.isNullOrEmpty()) {
                                Collections.sort(registryList)
                                registryList.joinToString(",")
                            } else {
                                null
                            }
                            group.addressList = addressListStr
                            group.updateTime = Date()
                            TaskPilotAdminBootstrap.instance.executorMapper.update(group)
                        }
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 执行器注册监控线程执行时发生异常。", e)
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(Const.BEAT_TIMEOUT.toLong())
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 执行器注册监控线程休眠时发生异常。", e)
                    }
                }
            }
            logger.info(">>>>>>>>>>> task-pilot 执行器注册监控线程已停止。")
        }
        registryMonitorThread.isDaemon = true
        registryMonitorThread.name = "task-pilot, admin ExecutorRegistryMonitorHelper-registryMonitorThread"
        registryMonitorThread.start()
    }

    fun stop() {
        toStop = true
        registryOrRemoveThreadPool.shutdownNow()
        registryMonitorThread.interrupt()
        try {
            registryMonitorThread.join()
        } catch (e: Throwable) {
            logger.error("停止执行器注册监控线程时发生异常。", e)
        }
    }

    /**
     * 注册请求只做基础参数校验，其余交给异步线程更新注册表。
     */
    fun registry(registryParam: RegistryRequest): Response<String> {
        if (StringTool.isBlank(registryParam.registryGroup) ||
            StringTool.isBlank(registryParam.registryKey) ||
            StringTool.isBlank(registryParam.registryValue)
        ) {
            return Response.ofFail("Illegal Argument.")
        }

        registryOrRemoveThreadPool.execute {
            val ret = TaskPilotAdminBootstrap.instance.registryMapper.registrySaveOrUpdate(
                registryParam.registryGroup,
                registryParam.registryKey,
                registryParam.registryValue,
                Date()
            )
            if (ret == 1) {
                freshGroupRegistryInfo(registryParam)
            }
        }
        return Response.ofSuccess()
    }

    /**
     * 注销请求同样异步执行，避免摘除节点阻塞执行器心跳线程。
     */
    fun unregister(registryParam: RegistryRequest): Response<String> {
        if (StringTool.isBlank(registryParam.registryGroup) ||
            StringTool.isBlank(registryParam.registryKey) ||
            StringTool.isBlank(registryParam.registryValue)
        ) {
            return Response.ofFail("Illegal Argument.")
        }

        registryOrRemoveThreadPool.execute {
            val ret = TaskPilotAdminBootstrap.instance.registryMapper.registryDelete(
                registryParam.registryGroup,
                registryParam.registryKey,
                registryParam.registryValue
            )
            if (ret > 0) {
                freshGroupRegistryInfo(registryParam)
            }
        }
        return Response.ofSuccess()
    }

    /**
     * 该方法保留为空实现，与旧版一致，避免注册请求直接改动核心分组表。
     */
    private fun freshGroupRegistryInfo(registryParam: RegistryRequest) {
        // Under consideration, prevent affecting core tables
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExecutorRegistryHelper::class.java)
    }
}
