package com.ruishanio.taskpilot.admin.scheduler.config

import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.mapper.RegistryMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskLockMapper
import com.ruishanio.taskpilot.admin.mapper.TaskLogMapper
import com.ruishanio.taskpilot.admin.mapper.TaskReportMapper
import com.ruishanio.taskpilot.admin.scheduler.alarm.JobAlarmer
import com.ruishanio.taskpilot.admin.scheduler.complete.JobCompleter
import com.ruishanio.taskpilot.admin.scheduler.thread.JobCompleteHelper
import com.ruishanio.taskpilot.admin.scheduler.thread.JobFailAlarmMonitorHelper
import com.ruishanio.taskpilot.admin.scheduler.thread.JobLogReportHelper
import com.ruishanio.taskpilot.admin.scheduler.thread.JobRegistryHelper
import com.ruishanio.taskpilot.admin.scheduler.thread.JobScheduleHelper
import com.ruishanio.taskpilot.admin.scheduler.thread.JobTriggerPoolHelper
import com.ruishanio.taskpilot.admin.scheduler.trigger.JobTrigger
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 管理端运行时引导器。
 *
 * 统一承接管理端运行期所需的线程组件、Mapper 和远程调用配置。
 */
@Component
class TaskPilotAdminBootstrap : DisposableBean {
    lateinit var jobTriggerPoolHelper: JobTriggerPoolHelper
    lateinit var jobRegistryHelper: JobRegistryHelper
    lateinit var jobFailAlarmMonitorHelper: JobFailAlarmMonitorHelper
    lateinit var jobCompleteHelper: JobCompleteHelper
    lateinit var jobLogReportHelper: JobLogReportHelper
    lateinit var jobScheduleHelper: JobScheduleHelper

    @Volatile
    private var started: Boolean = false

    @Value("\${task-pilot.accessToken}")
    var accessToken: String = ""

    @Value("\${task-pilot.timeout}")
    var timeout: Int = 0

    @Value("\${spring.mail.from}")
    var emailFrom: String = ""

    @Value("\${task-pilot.triggerpool.fast.max}")
    private var triggerPoolFastMaxInternal: Int = 0

    @Value("\${task-pilot.triggerpool.slow.max}")
    private var triggerPoolSlowMaxInternal: Int = 0

    @Value("\${task-pilot.logretentiondays}")
    private var logretentiondaysInternal: Int = 0

    @Resource
    lateinit var taskLogMapper: TaskLogMapper

    @Resource
    lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    lateinit var registryMapper: RegistryMapper

    @Resource
    lateinit var executorMapper: ExecutorMapper

    @Resource
    lateinit var taskReportMapper: TaskReportMapper

    @Resource
    lateinit var taskLockMapper: TaskLockMapper

    @Resource
    lateinit var mailSender: JavaMailSender

    @Resource
    lateinit var transactionManager: PlatformTransactionManager

    @Resource
    lateinit var jobAlarmer: JobAlarmer

    @Resource
    lateinit var jobTrigger: JobTrigger

    @Resource
    lateinit var jobCompleter: JobCompleter

    /**
     * 统一收敛触发线程池下限，防止配置过低时影响调度吞吐。
     */
    val triggerPoolFastMax: Int
        get() = if (triggerPoolFastMaxInternal < 200) 200 else triggerPoolFastMaxInternal

    /**
     * 慢线程池同样保留最低容量保护。
     */
    val triggerPoolSlowMax: Int
        get() = if (triggerPoolSlowMaxInternal < 100) 100 else triggerPoolSlowMaxInternal

    /**
     * 日志保留天数小于 3 时按关闭处理，保持旧版容错语义。
     */
    val logretentiondays: Int
        get() = if (logretentiondaysInternal < 3) -1 else logretentiondaysInternal

    /**
     * 先尽早暴露 bootstrap 单例，保证应用就绪后各线程和运行期服务能通过 companion 访问配置。
     */
    @PostConstruct
    fun init() {
        adminConfig = this
    }

    /**
     * 延迟到 Spring Boot 完全就绪后再启动后台线程，避免与 Flyway 建表迁移竞争。
     */
    @EventListener(ApplicationReadyEvent::class)
    @Throws(Exception::class)
    fun onApplicationReady() {
        if (started) {
            return
        }
        doStart()
        started = true
    }

    @Throws(Exception::class)
    override fun destroy() {
        doStop()
    }

    /**
     * 按依赖顺序启动所有线程组件，避免回调或调度线程先于线程池可用。
     */
    @Throws(Exception::class)
    private fun doStart() {
        jobTriggerPoolHelper = JobTriggerPoolHelper().also { it.start() }
        jobRegistryHelper = JobRegistryHelper().also { it.start() }
        jobFailAlarmMonitorHelper = JobFailAlarmMonitorHelper().also { it.start() }
        jobCompleteHelper = JobCompleteHelper().also { it.start() }
        jobLogReportHelper = JobLogReportHelper().also { it.start() }
        jobScheduleHelper = JobScheduleHelper().also { it.start() }
        logger.info(">>>>>>>>> task-pilot 管理端启动完成。")
    }

    /**
     * 停止阶段按启动逆序回收组件，避免仍在调度中的线程依赖已关闭资源。
     */
    private fun doStop() {
        started = false
        if (::jobScheduleHelper.isInitialized) {
            jobScheduleHelper.stop()
        }
        if (::jobLogReportHelper.isInitialized) {
            jobLogReportHelper.stop()
        }
        if (::jobCompleteHelper.isInitialized) {
            jobCompleteHelper.stop()
        }
        if (::jobFailAlarmMonitorHelper.isInitialized) {
            jobFailAlarmMonitorHelper.stop()
        }
        if (::jobRegistryHelper.isInitialized) {
            jobRegistryHelper.stop()
        }
        if (::jobTriggerPoolHelper.isInitialized) {
            jobTriggerPoolHelper.stop()
        }
        logger.info(">>>>>>>>> task-pilot 管理端已停止。")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotAdminBootstrap::class.java)

        @Volatile
        private var adminConfig: TaskPilotAdminBootstrap? = null

        private val executorBizRepository: ConcurrentMap<String, ExecutorBiz> = ConcurrentHashMap()

        val instance: TaskPilotAdminBootstrap
            get() = adminConfig ?: throw IllegalStateException("task-pilot admin bootstrap not initialized.")

        /**
         * 执行器客户端按地址缓存，避免每次触发都重新构建 HTTP 代理。
         */
        @Throws(Exception::class)
        fun getExecutorBiz(address: String?): ExecutorBiz? {
            if (StringTool.isBlank(address)) {
                return null
            }

            // 执行器地址约定为服务根地址，这里只做基础清洗，避免缓存 key 因尾部斜杠分裂。
            var normalizedAddress = address!!.trim()
            normalizedAddress = StringTool.removeSuffix(normalizedAddress, "/") ?: normalizedAddress
            executorBizRepository[normalizedAddress]?.let { return it }

            val executorBiz = HttpTool.createClient()
                .url(normalizedAddress)
                .timeout(instance.timeout * 1000)
                .header(Const.TASK_PILOT_ACCESS_TOKEN, instance.accessToken)
                .proxy(ExecutorBiz::class.java)
            executorBizRepository[normalizedAddress] = executorBiz
            return executorBiz
        }
    }
}
