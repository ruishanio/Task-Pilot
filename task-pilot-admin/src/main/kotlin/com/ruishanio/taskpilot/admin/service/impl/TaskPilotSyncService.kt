package com.ruishanio.taskpilot.admin.service.impl

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.mapper.RegistryMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.model.Registry
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.core.openapi.model.SyncRequest
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date

/**
 * 处理 Starter 发起的执行器与任务定义同步。
 *
 * 设计取舍：
 * 1、对声明了同步元数据的任务，注解配置视为事实来源，已存在任务也会按声明值同步；
 * 2、执行器分组创建后会主动回填当前存活注册地址，避免首次自动建组时地址列表为空；
 * 3、只在检测到字段差异时才触发更新，避免应用每次重启都刷写任务更新时间与操作日志。
 */
@Service
class TaskPilotSyncService {
    @Resource
    private lateinit var executorMapper: ExecutorMapper

    @Resource
    private lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    private lateinit var registryMapper: RegistryMapper

    @Resource
    private lateinit var taskPilotService: TaskPilotService

    /**
     * 补齐执行器分组和任务定义，并返回汇总信息供执行器启动日志记录。
     */
    fun sync(request: SyncRequest?): Response<String> {
        if (request == null || StringTool.isBlank(request.appName)) {
            return Response.ofFail("sync fail, appName empty.")
        }

        val executor = initOrLoadGroup(request)
        if (executor == null || executor.id < 1) {
            return Response.ofFail("sync fail, executor group init failed.")
        }

        val systemLoginInfo = buildSystemLoginInfo()
        var createdTaskCount = 0
        var updatedTaskCount = 0
        var skippedTaskCount = 0
        val warnings = mutableListOf<String>()

        for (task in safeTasks(request.tasks)) {
            if (StringTool.isBlank(task.executorHandler)) {
                warnings.add("skip blank executorHandler")
                continue
            }

            val taskName = if (StringTool.isNotBlank(task.taskName)) task.taskName!!.trim() else task.executorHandler!!.trim()
            val existsTask = taskInfoMapper.loadByExecutorIdAndTaskName(executor.id, taskName)
            if (existsTask != null) {
                val expectedTask = buildTaskInfo(executor.id, task).apply {
                    id = existsTask.id
                }
                if (!needsTaskUpdate(existsTask, expectedTask)) {
                    skippedTaskCount++
                    continue
                }

                val updateResponse = taskPilotService.update(expectedTask, systemLoginInfo)
                if (!Response.isSuccess(updateResponse)) {
                    return Response.ofFail(
                        "sync task update fail, handler=${task.executorHandler}, msg=${updateResponse.msg ?: "null"}"
                    )
                }
                updatedTaskCount++
                continue
            }

            val jobInfo = buildTaskInfo(executor.id, task)
            val addResponse = taskPilotService.add(jobInfo, systemLoginInfo)
            if (!Response.isSuccess(addResponse)) {
                return Response.ofFail("sync task fail, handler=${task.executorHandler}, msg=${addResponse.msg ?: "null"}")
            }

            createdTaskCount++
            if (task.autoStart) {
                val startResponse = taskPilotService.start(jobInfo.id, systemLoginInfo)
                if (!Response.isSuccess(startResponse)) {
                    warnings.add("handler=${task.executorHandler} autoStart skipped: ${startResponse.msg ?: "null"}")
                }
            }
        }

        val summary = buildString {
            append("groupId=").append(executor.id)
            append(", createdTaskCount=").append(createdTaskCount)
            append(", updatedTaskCount=").append(updatedTaskCount)
            append(", skippedTaskCount=").append(skippedTaskCount)
            if (warnings.isNotEmpty()) {
                append(", warnings=").append(warnings)
            }
        }
        logger.info(">>>>>>>>>>> task-pilot 同步完成，appName={}, summary={}", request.appName, summary)
        return Response.ofSuccess(summary)
    }

    /**
     * 分组不存在时创建，已存在时仅对同步托管分组做最小更新。
     */
    private fun initOrLoadGroup(request: SyncRequest): Executor? {
        var group = executorMapper.loadByAppname(request.appName!!.trim())
        if (group == null) {
            group = Executor().apply {
                appname = request.appName!!.trim()
                title = buildGroupTitle(request)
                addressType = 0
                addressList = buildRegistryAddressList(request.appName)
                updateTime = Date()
            }
            executorMapper.save(group)
            return group
        }

        if (group.addressType == 0) {
            var changed = false
            val groupTitle = buildGroupTitle(request)
            if (!equalsNullable(group.title, groupTitle)) {
                group.title = groupTitle
                changed = true
            }

            val addressList = buildRegistryAddressList(group.appname)
            if (!equalsNullable(group.addressList, addressList)) {
                group.addressList = addressList
                changed = true
            }

            if (changed) {
                group.updateTime = Date()
                executorMapper.update(group)
            }
        }
        return group
    }

    /**
     * 仅填充同步托管字段，避免启动同步越权覆盖人工维护信息。
     */
    private fun buildTaskInfo(executorId: Int, task: SyncRequest.Task): TaskInfo =
        TaskInfo().apply {
            this.executorId = executorId
            taskName = if (StringTool.isNotBlank(task.taskName)) task.taskName!!.trim() else task.executorHandler!!.trim()
            taskDesc = if (StringTool.isNotBlank(task.taskDesc)) task.taskDesc!!.trim() else task.executorHandler!!.trim()
            author = if (StringTool.isNotBlank(task.author)) task.author!!.trim() else SYSTEM_OPERATOR
            alarmEmail = task.alarmEmail
            scheduleType = defaultEnum(task.scheduleType, ScheduleTypeEnum.CRON)
            scheduleConf = if (StringTool.isNotBlank(task.scheduleConf)) task.scheduleConf!!.trim() else ""
            misfireStrategy = defaultEnum(task.misfireStrategy, MisfireStrategyEnum.DO_NOTHING)
            executorRouteStrategy = defaultEnum(task.executorRouteStrategy, ExecutorRouteStrategyEnum.FIRST)
            executorHandler = task.executorHandler!!.trim()
            executorParam = task.executorParam
            executorBlockStrategy = defaultEnum(task.executorBlockStrategy, ExecutorBlockStrategyEnum.SERIAL_EXECUTION)
            executorTimeout = task.executorTimeout
            executorFailRetryCount = task.executorFailRetryCount
            glueType = GlueTypeEnum.BEAN.name
            glueSource = ""
            glueRemark = "注解同步"
            childTaskId = task.childTaskId
        }

    /**
     * 仅比较同步托管字段，避免无差异更新污染操作日志。
     */
    private fun needsTaskUpdate(existsTask: TaskInfo, expectedTask: TaskInfo): Boolean =
        !equalsNullable(existsTask.taskName, expectedTask.taskName) ||
            !equalsNullable(existsTask.taskDesc, expectedTask.taskDesc) ||
            !equalsNullable(existsTask.author, expectedTask.author) ||
            !equalsNullable(existsTask.alarmEmail, expectedTask.alarmEmail) ||
            !equalsNullable(existsTask.scheduleType, expectedTask.scheduleType) ||
            !equalsNullable(existsTask.scheduleConf, expectedTask.scheduleConf) ||
            !equalsNullable(existsTask.misfireStrategy, expectedTask.misfireStrategy) ||
            !equalsNullable(existsTask.executorRouteStrategy, expectedTask.executorRouteStrategy) ||
            !equalsNullable(existsTask.executorHandler, expectedTask.executorHandler) ||
            !equalsNullable(existsTask.executorParam, expectedTask.executorParam) ||
            !equalsNullable(existsTask.executorBlockStrategy, expectedTask.executorBlockStrategy) ||
            existsTask.executorTimeout != expectedTask.executorTimeout ||
            existsTask.executorFailRetryCount != expectedTask.executorFailRetryCount ||
            !equalsNullable(existsTask.childTaskId, expectedTask.childTaskId)

    /**
     * 启动同步通过系统身份复用现有服务层的校验与审计逻辑。
     */
    private fun buildSystemLoginInfo(): LoginInfo =
        LoginInfo().apply {
            userId = "0"
            userName = SYSTEM_OPERATOR
            realName = "TaskPilot Sync"
            roleList = listOf(Consts.ADMIN_ROLE)
            expireTime = Long.MAX_VALUE
            signature = SYSTEM_OPERATOR
        }

    /**
     * 回填当前在线执行器地址，避免新建分组后需要等待监控线程下一轮同步。
     */
    private fun buildRegistryAddressList(appname: String?): String? {
        val registryList: List<Registry> = registryMapper.findAll(Const.DEAD_TIMEOUT, Date())
        if (registryList.isEmpty()) {
            return null
        }

        val addressList = registryList.asSequence()
            .filter { RegistType.EXECUTOR.name == it.registryGroup }
            .filter { appname == it.registryKey }
            .mapNotNull { it.registryValue }
            .distinct()
            .sorted()
            .toList()
        return if (addressList.isEmpty()) null else addressList.joinToString(",")
    }

    /**
     * 标题为空时回退为截断后的 appName，避免超出数据库字段长度。
     */
    private fun buildGroupTitle(request: SyncRequest): String {
        val title = if (StringTool.isNotBlank(request.groupTitle)) request.groupTitle!!.trim() else request.appName!!.trim()
        return if (title.length > 12) title.substring(0, 12) else title
    }

    /**
     * 空任务列表统一转为空集合，避免调用方额外判空。
     */
    private fun safeTasks(tasks: List<SyncRequest.Task>?): List<SyncRequest.Task> = tasks ?: emptyList()

    /**
     * 同步请求里的枚举字段允许缺省，统一在这里回退到平台默认值。
     */
    private fun <T : Enum<T>> defaultEnum(value: T?, defaultValue: T): T = value ?: defaultValue

    /**
     * 简单值对比同时兼容 null 场景，便于统一比较字符串与枚举字段。
     */
    private fun equalsNullable(left: Any?, right: Any?): Boolean = left == right

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotSyncService::class.java)
        private const val SYSTEM_OPERATOR = "task-pilot-sync"
    }
}
