package com.ruishanio.taskpilot.admin.service.impl

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.TaskPilotGroupMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotRegistryMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.model.TaskPilotRegistry
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
    private lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Resource
    private lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Resource
    private lateinit var taskPilotRegistryMapper: TaskPilotRegistryMapper

    @Resource
    private lateinit var taskPilotService: TaskPilotService

    /**
     * 补齐执行器分组和任务定义，并返回汇总信息供执行器启动日志记录。
     */
    fun sync(request: SyncRequest?): Response<String> {
        if (request == null || StringTool.isBlank(request.appName)) {
            return Response.ofFail("sync fail, appName empty.")
        }

        val taskPilotGroup = initOrLoadGroup(request)
        if (taskPilotGroup == null || taskPilotGroup.id < 1) {
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

            val existsTask = taskPilotInfoMapper.loadByGroupAndExecutorHandler(taskPilotGroup.id, task.executorHandler!!.trim())
            if (existsTask != null) {
                val expectedTask = buildTaskInfo(taskPilotGroup.id, task).apply {
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

            val jobInfo = buildTaskInfo(taskPilotGroup.id, task)
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
            append("groupId=").append(taskPilotGroup.id)
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
    private fun initOrLoadGroup(request: SyncRequest): TaskPilotGroup? {
        var group = taskPilotGroupMapper.loadByAppname(request.appName!!.trim())
        if (group == null) {
            group = TaskPilotGroup().apply {
                appname = request.appName!!.trim()
                title = buildGroupTitle(request)
                addressType = 0
                addressList = buildRegistryAddressList(request.appName)
                updateTime = Date()
            }
            taskPilotGroupMapper.save(group)
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
                taskPilotGroupMapper.update(group)
            }
        }
        return group
    }

    /**
     * 仅填充同步托管字段，避免启动同步越权覆盖人工维护信息。
     */
    private fun buildTaskInfo(jobGroupId: Int, task: SyncRequest.Task): TaskPilotInfo =
        TaskPilotInfo().apply {
            jobGroup = jobGroupId
            jobDesc = if (StringTool.isNotBlank(task.jobDesc)) task.jobDesc!!.trim() else task.executorHandler!!.trim()
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
            childJobId = task.childJobId
        }

    /**
     * 仅比较同步托管字段，避免无差异更新污染操作日志。
     */
    private fun needsTaskUpdate(existsTask: TaskPilotInfo, expectedTask: TaskPilotInfo): Boolean =
        !equalsNullable(existsTask.jobDesc, expectedTask.jobDesc) ||
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
            !equalsNullable(existsTask.childJobId, expectedTask.childJobId)

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
        val registryList: List<TaskPilotRegistry> = taskPilotRegistryMapper.findAll(Const.DEAD_TIMEOUT, Date())
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
