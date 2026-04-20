package com.ruishanio.taskpilot.core.executor

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.handler.ITaskHandler
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot
import com.ruishanio.taskpilot.core.handler.impl.MethodTaskHandler
import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.server.EmbedServer
import com.ruishanio.taskpilot.core.thread.TaskLogFileCleanThread
import com.ruishanio.taskpilot.core.thread.TaskThread
import com.ruishanio.taskpilot.core.thread.TriggerCallbackThread
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.http.IPTool
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

/**
 * TaskPilot 执行器基类。
 *
 * 负责初始化日志、RPC 客户端、回调线程和嵌入式服务，并维护任务处理器与任务线程仓库。
 */
open class TaskPilotExecutor {
    var adminAddresses: String? = null
    var accessToken: String? = null
    var timeout: Int = 0
    var enabled: Boolean? = null
    var appname: String? = null
    var address: String? = null
    var ip: String? = null
    var port: Int = 0
    var logPath: String? = null
    var logRetentionDays: Int = 0

    /**
     * 启动执行器基础设施。
     */
    @Throws(Exception::class)
    open fun start() {
        if (enabled != null && enabled == false) {
            logger.info(">>>>>>>>>>> task-pilot 执行器未启动，enabled={}", enabled)
            return
        }

        TaskPilotFileAppender.initLogPath(logPath)
        initAdminBizList(adminAddresses, accessToken, timeout)
        TaskLogFileCleanThread.getInstance().start(logRetentionDays.toLong())
        TriggerCallbackThread.getInstance().start()
        initEmbedServer(address, ip, port, appname, accessToken)
    }

    /**
     * 停止执行器并释放资源。
     */
    open fun destroy() {
        stopEmbedServer()

        if (taskThreadRepository.isNotEmpty()) {
            try {
                TimeUnit.SECONDS.sleep(ELEGANT_SHUTDOWN_WAITING_SECONDS)
            } catch (e: Throwable) {
                logger.error(">>>>>>>>>>> task-pilot 执行器优雅关闭等待任务完成时发生异常。", e)
            }

            for ((taskId, _) in taskThreadRepository) {
                val oldTaskThread = removeTaskThread(taskId, "web container destroy and kill the task.")
                if (oldTaskThread != null) {
                    try {
                        oldTaskThread.join()
                    } catch (e: InterruptedException) {
                        logger.error(">>>>>>>>>>> task-pilot 销毁任务线程并等待结束时发生异常，taskId={}", taskId, e)
                    }
                }
            }
            taskThreadRepository.clear()
        }
        taskHandlerRepository.clear()

        TaskLogFileCleanThread.getInstance().toStop()
        TriggerCallbackThread.getInstance().toStop()
    }

    /**
     * 初始化调度中心客户端代理列表。
     */
    @Throws(Exception::class)
    private fun initAdminBizList(adminAddresses: String?, accessToken: String?, timeout: Int) {
        if (StringTool.isBlank(adminAddresses)) {
            return
        }

        val adminBizCollection = adminBizList ?: ArrayList<AdminBiz>().also { adminBizList = it }
        for (address in adminAddresses!!.trim().split(",")) {
            if (StringTool.isBlank(address)) {
                continue
            }

            // 调度中心地址约定为服务根地址，这里只清理尾部斜杠，避免客户端缓存被不同写法打散。
            var finalAddress = address.trim()
            finalAddress = StringTool.removeSuffix(finalAddress, "/") ?: finalAddress
            val finalTimeout = if (timeout in 1..10) timeout else 3

            val adminBiz =
                HttpTool.createClient()
                    .url(finalAddress)
                    .timeout(finalTimeout * 1000)
                    .header(Const.TASK_PILOT_ACCESS_TOKEN, accessToken)
                    .proxy(AdminBiz::class.java)
            adminBizCollection.add(adminBiz)
        }
    }

    private var embedServer: EmbedServer? = null

    /**
     * 启动嵌入式 RPC 服务。
     */
    @Throws(Exception::class)
    private fun initEmbedServer(
        address: String?,
        ip: String?,
        port: Int,
        appname: String?,
        accessToken: String?
    ) {
        val finalPort = if (port > 0) port else IPTool.getAvailablePort(9999)
        val finalIp = if (StringTool.isNotBlank(ip)) ip else IPTool.getIp()

        var finalAddress = address
        if (StringTool.isBlank(finalAddress)) {
            val ipPortAddress = IPTool.toAddressString(finalIp, finalPort)
            finalAddress = "http://{ip_port}/".replace("{ip_port}", ipPortAddress)
        }

        if (StringTool.isBlank(accessToken)) {
            logger.warn(">>>>>>>>>>> task-pilot accessToken 为空。为保证系统安全，请配置 accessToken。")
        }

        embedServer = EmbedServer().also {
            it.start(finalAddress, finalPort, appname, accessToken)
        }
    }

    /**
     * 停止嵌入式 RPC 服务。
     */
    private fun stopEmbedServer() {
        try {
            embedServer?.stop()
        } catch (e: Exception) {
            logger.error(">>>>>>>>>>> task-pilot 停止嵌入式服务时发生异常。", e)
        }
    }

    /**
     * 注册方法型任务处理器。
     */
    protected fun registerTaskHandler(taskPilot: TaskPilot?, bean: Any, executeMethod: Method) {
        if (taskPilot == null) {
            return
        }

        val name = taskPilot.value
        val clazz = bean.javaClass
        val methodName = executeMethod.name
        if (name.trim().isEmpty()) {
            throw RuntimeException("task-pilot method-taskhandler name invalid, for[$clazz#$methodName] .")
        }
        if (loadTaskHandler(name) != null) {
            throw RuntimeException("task-pilot taskhandler[$name] naming conflicts.")
        }

        executeMethod.isAccessible = true

        var initMethod: Method? = null
        var destroyMethod: Method? = null

        if (taskPilot.init.trim().isNotEmpty()) {
            try {
                initMethod = clazz.getDeclaredMethod(taskPilot.init)
                initMethod.isAccessible = true
            } catch (_: NoSuchMethodException) {
                throw RuntimeException("task-pilot method-taskhandler initMethod invalid, for[$clazz#$methodName] .")
            }
        }
        if (taskPilot.destroy.trim().isNotEmpty()) {
            try {
                destroyMethod = clazz.getDeclaredMethod(taskPilot.destroy)
                destroyMethod.isAccessible = true
            } catch (_: NoSuchMethodException) {
                throw RuntimeException("task-pilot method-taskhandler destroyMethod invalid, for[$clazz#$methodName] .")
            }
        }

        registerTaskHandler(name, MethodTaskHandler(bean, executeMethod, initMethod, destroyMethod))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotExecutor::class.java)
        private const val ELEGANT_SHUTDOWN_WAITING_SECONDS: Long = 5

        private var adminBizList: MutableList<AdminBiz>? = null
        private val taskHandlerRepository: ConcurrentMap<String, ITaskHandler> = ConcurrentHashMap()
        private val taskThreadRepository: ConcurrentMap<Int, TaskThread> = ConcurrentHashMap()

        /**
         * 获取调度中心客户端列表。
         */
        fun getAdminBizList(): MutableList<AdminBiz>? = adminBizList

        /**
         * 按名称加载任务处理器。
         */
        fun loadTaskHandler(name: String): ITaskHandler? = taskHandlerRepository[name]

        /**
         * 注册任务处理器。
         */
        fun registerTaskHandler(name: String, taskHandler: ITaskHandler): ITaskHandler? {
            logger.info(">>>>>>>>>>> task-pilot 注册任务处理器成功，name={}, taskHandler={}", name, taskHandler)
            return taskHandlerRepository.put(name, taskHandler)
        }

        /**
         * 注册任务线程，并在必要时替换旧线程。
         */
        fun registerTaskThread(taskId: Int, handler: ITaskHandler, removeOldReason: String?): TaskThread {
            val newTaskThread = TaskThread(taskId, handler)
            newTaskThread.start()
            logger.info(">>>>>>>>>>> task-pilot 注册任务线程成功，taskId={}, handler={}", taskId, handler)

            val oldTaskThread = taskThreadRepository.put(taskId, newTaskThread)
            if (oldTaskThread != null) {
                oldTaskThread.toStop(removeOldReason)
                oldTaskThread.interrupt()
            }

            return newTaskThread
        }

        /**
         * 移除任务线程。
         */
        fun removeTaskThread(taskId: Int, removeOldReason: String?): TaskThread? {
            val oldTaskThread = taskThreadRepository.remove(taskId)
            if (oldTaskThread != null) {
                oldTaskThread.toStop(removeOldReason)
                oldTaskThread.interrupt()
                return oldTaskThread
            }
            return null
        }

        /**
         * 按任务 ID 加载线程。
         */
        fun loadTaskThread(taskId: Int): TaskThread? = taskThreadRepository[taskId]
    }
}
