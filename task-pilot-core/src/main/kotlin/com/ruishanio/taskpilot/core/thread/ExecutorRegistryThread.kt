package com.ruishanio.taskpilot.core.thread

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 执行器注册线程。
 *
 * 周期性向调度中心续约，停止时再做一次摘除注册。
 */
class ExecutorRegistryThread private constructor() {
    private var registryThread: Thread? = null
    @Volatile
    private var toStop: Boolean = false

    fun start(appname: String?, address: String?) {
        if (appname.isNullOrBlank()) {
            logger.warn(">>>>>>>>>>> task-pilot，执行器注册配置无效，appname 为空。")
            return
        }
        if (TaskPilotExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> task-pilot，执行器注册配置无效，adminAddresses 为空。")
            return
        }

        registryThread =
            Thread {
                while (!toStop) {
                    try {
                        val registryParam = RegistryRequest(RegistType.EXECUTOR.name, appname, address)
                        for (adminBiz in TaskPilotExecutor.getAdminBizList().orEmpty()) {
                            try {
                                val registryResult = adminBiz.registry(registryParam)
                                if (registryResult.isSuccess) {
                                    logger.debug(
                                        ">>>>>>>>>>> task-pilot 执行器注册成功，registryParam:{}, registryResult:{}",
                                        registryParam,
                                        Response.ofSuccess<String>()
                                    )
                                    break
                                } else {
                                    logger.info(
                                        ">>>>>>>>>>> task-pilot 执行器注册失败，registryParam:{}, registryResult:{}",
                                        registryParam,
                                        registryResult
                                    )
                                }
                            } catch (e: Throwable) {
                                logger.info(">>>>>>>>>>> task-pilot 执行器注册异常，registryParam:{}", registryParam, e)
                            }
                        }
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> task-pilot 执行器注册线程执行异常。", e)
                        }
                    }

                    try {
                        if (!toStop) {
                            TimeUnit.SECONDS.sleep(Const.BEAT_TIMEOUT.toLong())
                        }
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.warn(">>>>>>>>>>> task-pilot，执行器注册线程被中断。", e)
                        }
                    }
                }

                try {
                    val registryParam = RegistryRequest(RegistType.EXECUTOR.name, appname, address)
                    for (adminBiz in TaskPilotExecutor.getAdminBizList().orEmpty()) {
                        try {
                            val registryResult = adminBiz.registryRemove(registryParam)
                            if (registryResult.isSuccess) {
                                logger.info(
                                    ">>>>>>>>>>> task-pilot 执行器摘除注册成功，registryParam:{}, registryResult:{}",
                                    registryParam,
                                    Response.ofSuccess<String>()
                                )
                                break
                            } else {
                                logger.info(
                                    ">>>>>>>>>>> task-pilot 执行器摘除注册失败，registryParam:{}, registryResult:{}",
                                    registryParam,
                                    registryResult
                                )
                            }
                        } catch (e: Throwable) {
                            if (!toStop) {
                                logger.info(">>>>>>>>>>> task-pilot 执行器摘除注册异常，registryParam:{}", registryParam, e)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 执行器摘除注册时发生异常。", e)
                    }
                }
                logger.info(">>>>>>>>>>> task-pilot，执行器注册线程已销毁。")
            }.apply {
                isDaemon = true
                name = "task-pilot, executor ExecutorRegistryThread"
                start()
            }
    }

    fun toStop() {
        toStop = true
        val thread = registryThread ?: return
        thread.interrupt()
        try {
            thread.join()
        } catch (e: Throwable) {
            logger.error(">>>>>>>>>>> task-pilot 停止执行器注册线程时发生异常。", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExecutorRegistryThread::class.java)
        private val instance = ExecutorRegistryThread()
        fun getInstance(): ExecutorRegistryThread = instance
    }
}
