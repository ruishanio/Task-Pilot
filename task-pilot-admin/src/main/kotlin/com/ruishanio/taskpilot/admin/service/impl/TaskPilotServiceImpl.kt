package com.ruishanio.taskpilot.admin.service.impl

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.TriggerStatus
import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.mapper.GlueLogMapper
import com.ruishanio.taskpilot.admin.mapper.TaskLogMapper
import com.ruishanio.taskpilot.admin.mapper.TaskReportMapper
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.model.TaskReport
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.cron.CronExpression
import com.ruishanio.taskpilot.admin.scheduler.thread.JobScheduleHelper
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import com.ruishanio.taskpilot.admin.scheduler.type.toScheduleType
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.util.Date
import java.util.HashMap
import java.util.HashSet

/**
 * 管理端任务核心服务。
 *
 * 这里继续保留原有校验顺序和操作日志写入点，避免 Kotlin 迁移改变任务配置收敛与审计行为。
 */
@Service
class TaskPilotServiceImpl : TaskPilotService {
    @Resource
    private lateinit var executorMapper: ExecutorMapper

    @Resource
    private lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    lateinit var taskLogMapper: TaskLogMapper

    @Resource
    private lateinit var glueLogMapper: GlueLogMapper

    @Resource
    private lateinit var taskReportMapper: TaskReportMapper

    override fun pageList(
        offset: Int,
        pagesize: Int,
        executorId: Int,
        triggerStatus: Int,
        taskName: String?,
        taskDesc: String?,
        executorHandler: String?,
        author: String?
    ): Response<PageModel<TaskInfo>> {
        val list = taskInfoMapper.pageList(offset, pagesize, executorId, triggerStatus, taskName, taskDesc, executorHandler, author)
        val listCount = taskInfoMapper.pageListCount(offset, pagesize, executorId, triggerStatus, taskName, taskDesc, executorHandler, author)

        val pageModel = PageModel<TaskInfo>()
        pageModel.data = list
        pageModel.total = listCount
        return Response.ofSuccess(pageModel)
    }

    override fun add(taskInfo: TaskInfo, loginInfo: LoginInfo): Response<String> {
        normalizeTaskInfoKeyFields(taskInfo)
        val baseValidResult = validBaseAndTrigger(taskInfo)
        if (!baseValidResult.isSuccess) {
            return baseValidResult
        }

        val jobValidResult = validJob(taskInfo)
        if (!jobValidResult.isSuccess) {
            return jobValidResult
        }

        val advancedValidResult = validAdvanced(taskInfo)
        if (!advancedValidResult.isSuccess) {
            return advancedValidResult
        }

        val uniqueValidResult = validUniqueFields(taskInfo, null)
        if (!uniqueValidResult.isSuccess) {
            return uniqueValidResult
        }

        val childTaskValidResult = validAndNormalizeChildTask(taskInfo, loginInfo, false)
        if (!childTaskValidResult.isSuccess) {
            return childTaskValidResult
        }

        taskInfo.addTime = Date()
        taskInfo.updateTime = Date()
        taskInfo.glueUpdateTime = Date()
        taskInfoMapper.save(taskInfo)
        if (taskInfo.id < 1) {
            return Response.ofFail("新增失败")
        }

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobinfo-save",
            GsonTool.toJson(taskInfo)
        )
        return Response.ofSuccess(taskInfo.id.toString())
    }

    override fun update(taskInfo: TaskInfo, loginInfo: LoginInfo): Response<String> {
        normalizeTaskInfoKeyFields(taskInfo)
        if (StringTool.isBlank(taskInfo.taskName)) {
            return Response.ofFail("请输入任务名称")
        }
        if (taskInfo.taskName!!.length > 128) {
            return Response.ofFail("任务名称长度不能超过128")
        }
        if (StringTool.isBlank(taskInfo.taskDesc)) {
            return Response.ofFail("请输入任务描述")
        }
        if (StringTool.isBlank(taskInfo.author)) {
            return Response.ofFail("请输入负责人")
        }

        val scheduleTypeEnum = taskInfo.scheduleType ?: return Response.ofFail("调度类型非法")
        val triggerValidResult = validSchedule(taskInfo, scheduleTypeEnum)
        if (!triggerValidResult.isSuccess) {
            return triggerValidResult
        }

        val advancedValidResult = validAdvanced(taskInfo)
        if (!advancedValidResult.isSuccess) {
            return advancedValidResult
        }

        val uniqueValidResult = validUniqueFields(taskInfo, taskInfo.id)
        if (!uniqueValidResult.isSuccess) {
            return uniqueValidResult
        }

        val childTaskValidResult = validAndNormalizeChildTask(taskInfo, loginInfo, true)
        if (!childTaskValidResult.isSuccess) {
            return childTaskValidResult
        }

        val executor = executorMapper.load(taskInfo.executorId)
            ?: return Response.ofFail("执行器非法")

        val existsJobInfo = taskInfoMapper.loadById(taskInfo.id)
            ?: return Response.ofFail("任务ID不存在")

        var nextTriggerTime = existsJobInfo.triggerNextTime
        val scheduleDataNotChanged = taskInfo.scheduleType == existsJobInfo.scheduleType &&
            taskInfo.scheduleConf == existsJobInfo.scheduleConf
        if (existsJobInfo.triggerStatus == TriggerStatus.RUNNING.value && !scheduleDataNotChanged) {
            try {
                val nextValidTime = scheduleTypeEnum.toScheduleType().generateNextTriggerTime(
                    taskInfo,
                    Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS)
                ) ?: return Response.ofFail("调度类型非法")
                nextTriggerTime = nextValidTime.time
            } catch (e: Exception) {
                logger.error("重新计算任务下次触发时间时发生异常。taskId={}", taskInfo.id, e)
                return Response.ofFail("调度类型非法")
            }
        }

        existsJobInfo.executorId = executor.id
        existsJobInfo.taskName = taskInfo.taskName
        existsJobInfo.taskDesc = taskInfo.taskDesc
        existsJobInfo.author = taskInfo.author
        existsJobInfo.alarmEmail = taskInfo.alarmEmail
        existsJobInfo.scheduleType = taskInfo.scheduleType
        existsJobInfo.scheduleConf = taskInfo.scheduleConf
        existsJobInfo.misfireStrategy = taskInfo.misfireStrategy
        existsJobInfo.executorRouteStrategy = taskInfo.executorRouteStrategy
        existsJobInfo.executorHandler = taskInfo.executorHandler?.trim()
        existsJobInfo.executorParam = taskInfo.executorParam
        existsJobInfo.executorBlockStrategy = taskInfo.executorBlockStrategy
        existsJobInfo.executorTimeout = taskInfo.executorTimeout
        existsJobInfo.executorFailRetryCount = taskInfo.executorFailRetryCount
        existsJobInfo.childTaskId = taskInfo.childTaskId
        existsJobInfo.triggerNextTime = nextTriggerTime
        existsJobInfo.updateTime = Date()
        taskInfoMapper.update(existsJobInfo)

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobinfo-update",
            GsonTool.toJson(existsJobInfo)
        )
        return Response.ofSuccess()
    }

    override fun remove(id: Int, loginInfo: LoginInfo): Response<String> {
        val taskInfo = taskInfoMapper.loadById(id) ?: return Response.ofSuccess()
        if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, taskInfo.executorId)) {
            return Response.ofFail("权限拦截")
        }

        taskInfoMapper.delete(id.toLong())
        taskLogMapper.delete(id)
        glueLogMapper.deleteByTaskId(id)

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobinfo-remove",
            id
        )
        return Response.ofSuccess()
    }

    override fun start(id: Int, loginInfo: LoginInfo): Response<String> {
        val taskInfo = taskInfoMapper.loadById(id)
            ?: return Response.ofFail("任务ID非法")
        if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, taskInfo.executorId)) {
            return Response.ofFail("权限拦截")
        }

        val scheduleTypeEnum = taskInfo.scheduleType ?: ScheduleTypeEnum.NONE
        if (scheduleTypeEnum == ScheduleTypeEnum.NONE) {
            return Response.ofFail("当前调度类型禁止启动")
        }

        val nextTriggerTime = try {
            val nextValidTime = scheduleTypeEnum.toScheduleType().generateNextTriggerTime(
                taskInfo,
                Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS)
            ) ?: return Response.ofFail("调度类型非法")
            nextValidTime.time
        } catch (e: Exception) {
            logger.error("启动任务并计算下次触发时间时发生异常。jobId={}", id, e)
            return Response.ofFail("调度类型非法")
        }

        taskInfo.triggerStatus = TriggerStatus.RUNNING.value
        taskInfo.triggerLastTime = 0
        taskInfo.triggerNextTime = nextTriggerTime
        taskInfo.updateTime = Date()
        taskInfoMapper.update(taskInfo)

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobinfo-start",
            id
        )
        return Response.ofSuccess()
    }

    override fun stop(id: Int, loginInfo: LoginInfo): Response<String> {
        val taskInfo = taskInfoMapper.loadById(id)
            ?: return Response.ofFail("任务ID非法")
        if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, taskInfo.executorId)) {
            return Response.ofFail("权限拦截")
        }

        taskInfo.triggerStatus = TriggerStatus.STOPPED.value
        taskInfo.triggerLastTime = 0
        taskInfo.triggerNextTime = 0
        taskInfo.updateTime = Date()
        taskInfoMapper.update(taskInfo)

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobinfo-stop",
            id
        )
        return Response.ofSuccess()
    }

    override fun trigger(
        loginInfo: LoginInfo,
        taskId: Int,
        executorParam: String?,
        addressList: String?
    ): Response<String> {
        val taskInfo = taskInfoMapper.loadById(taskId)
            ?: return Response.ofFail("任务ID非法")
        if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, taskInfo.executorId)) {
            return Response.ofFail("权限拦截")
        }

        TaskPilotAdminBootstrap.instance.jobTriggerPoolHelper.trigger(
            taskId,
            TriggerTypeEnum.MANUAL,
            -1,
            null,
            executorParam ?: "",
            addressList
        )

        logger.info(
            ">>>>>>>>>>> task-pilot 操作日志：operator={}, type={}, content={}",
            loginInfo.userName,
            "jobinfo-trigger",
            taskId
        )
        return Response.ofSuccess()
    }

    override fun dashboardInfo(): Map<String, Any> {
        val jobInfoCount = taskInfoMapper.findAllCount()
        var jobLogCount = 0
        var jobLogSuccessCount = 0
        val taskReport = taskReportMapper.queryLogReportTotal()
        if (taskReport != null) {
            jobLogCount = taskReport.runningCount + taskReport.sucCount + taskReport.failCount
            jobLogSuccessCount = taskReport.sucCount
        }

        val executorAddressSet = HashSet<String>()
        for (group in executorMapper.findAll()) {
            if (!group.registryList.isNullOrEmpty()) {
                executorAddressSet.addAll(group.registryList!!)
            }
        }

        val dashboardMap = HashMap<String, Any>()
        dashboardMap["jobInfoCount"] = jobInfoCount
        dashboardMap["jobLogCount"] = jobLogCount
        dashboardMap["jobLogSuccessCount"] = jobLogSuccessCount
        dashboardMap["executorCount"] = executorAddressSet.size
        return dashboardMap
    }

    override fun chartInfo(startDate: Date?, endDate: Date?): Response<Map<String, Any>> {
        val triggerDayList = ArrayList<String>()
        val triggerDayCountRunningList = ArrayList<Int>()
        val triggerDayCountSucList = ArrayList<Int>()
        val triggerDayCountFailList = ArrayList<Int>()
        var triggerCountRunningTotal = 0
        var triggerCountSucTotal = 0
        var triggerCountFailTotal = 0

        val logReportList = taskReportMapper.queryLogReport(startDate, endDate)
        if (logReportList.isNotEmpty()) {
            for (item in logReportList) {
                val day = DateTool.formatDate(item.triggerDay!!)
                val triggerDayCountRunning = item.runningCount
                val triggerDayCountSuc = item.sucCount
                val triggerDayCountFail = item.failCount

                triggerDayList.add(day)
                triggerDayCountRunningList.add(triggerDayCountRunning)
                triggerDayCountSucList.add(triggerDayCountSuc)
                triggerDayCountFailList.add(triggerDayCountFail)

                triggerCountRunningTotal += triggerDayCountRunning
                triggerCountSucTotal += triggerDayCountSuc
                triggerCountFailTotal += triggerDayCountFail
            }
        } else {
            for (i in -6..0) {
                triggerDayList.add(DateTool.formatDate(DateTool.addDays(Date(), i.toLong())))
                triggerDayCountRunningList.add(0)
                triggerDayCountSucList.add(0)
                triggerDayCountFailList.add(0)
            }
        }

        val result = HashMap<String, Any>()
        result["triggerDayList"] = triggerDayList
        result["triggerDayCountRunningList"] = triggerDayCountRunningList
        result["triggerDayCountSucList"] = triggerDayCountSucList
        result["triggerDayCountFailList"] = triggerDayCountFailList
        result["triggerCountRunningTotal"] = triggerCountRunningTotal
        result["triggerCountSucTotal"] = triggerCountSucTotal
        result["triggerCountFailTotal"] = triggerCountFailTotal
        return Response.ofSuccess(result)
    }

    /**
     * 基础字段和调度配置校验分离出来，避免新增与更新逻辑重复展开。
     */
    private fun validBaseAndTrigger(jobInfo: TaskInfo): Response<String> {
        executorMapper.load(jobInfo.executorId)
            ?: return Response.ofFail("请选择执行器")
        if (StringTool.isBlank(jobInfo.taskName)) {
            return Response.ofFail("请输入任务名称")
        }
        if (jobInfo.taskName!!.length > 128) {
            return Response.ofFail("任务名称长度不能超过128")
        }
        if (StringTool.isBlank(jobInfo.taskDesc)) {
            return Response.ofFail("请输入任务描述")
        }
        if (StringTool.isBlank(jobInfo.author)) {
            return Response.ofFail("请输入负责人")
        }

        val scheduleTypeEnum = jobInfo.scheduleType ?: return Response.ofFail("调度类型非法")
        return validSchedule(jobInfo, scheduleTypeEnum)
    }

    /**
     * 调度表达式合法性仍由既有 Cron/FixRate 规则决定，避免影响线上任务的保存行为。
     */
    private fun validSchedule(jobInfo: TaskInfo, scheduleTypeEnum: ScheduleTypeEnum): Response<String> {
        return when (scheduleTypeEnum) {
            ScheduleTypeEnum.CRON -> {
                if (jobInfo.scheduleConf == null || !CronExpression.isValidExpression(jobInfo.scheduleConf)) {
                    Response.ofFail("Cron非法")
                } else {
                    Response.ofSuccess()
                }
            }
            ScheduleTypeEnum.FIX_RATE -> {
                if (jobInfo.scheduleConf == null) {
                    Response.ofFail("调度类型")
                } else {
                    try {
                        val fixSecond = jobInfo.scheduleConf!!.toInt()
                        if (fixSecond < 1) {
                            Response.ofFail("调度类型非法")
                        } else {
                            Response.ofSuccess()
                        }
                    } catch (_: Exception) {
                        Response.ofFail("调度类型非法")
                    }
                }
            }
            else -> Response.ofSuccess()
        }
    }

    /**
     * 任务类型校验仍沿用旧规则，脚本类型只做换行归一化，不额外修改内容。
     */
    private fun validJob(jobInfo: TaskInfo): Response<String> {
        val glueType = GlueTypeEnum.match(jobInfo.glueType)
            ?: return Response.ofFail("运行模式非法")
        if (glueType == GlueTypeEnum.BEAN && StringTool.isBlank(jobInfo.executorHandler)) {
            return Response.ofFail("请输入JobHandler")
        }
        if (glueType == GlueTypeEnum.GLUE_SHELL && jobInfo.glueSource != null) {
            jobInfo.glueSource = jobInfo.glueSource!!.replace("\r", "")
        }
        return Response.ofSuccess()
    }

    /**
     * 唯一键校验前先统一裁剪并归一化空字符串，避免“空白字符不同”绕过唯一规则。
     */
    private fun normalizeTaskInfoKeyFields(jobInfo: TaskInfo) {
        jobInfo.taskName = jobInfo.taskName?.trim()
        jobInfo.taskDesc = jobInfo.taskDesc?.trim()
        jobInfo.author = jobInfo.author?.trim()
        jobInfo.executorHandler = jobInfo.executorHandler?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * 任务名称只要求在同一执行器下唯一，允许不同执行器复用相同任务名。
     */
    private fun validUniqueFields(jobInfo: TaskInfo, currentId: Int?): Response<String> {
        val existsTaskByName = taskInfoMapper.loadByExecutorIdAndTaskName(jobInfo.executorId, jobInfo.taskName)
        if (existsTaskByName != null && existsTaskByName.id != currentId) {
            return Response.ofFail("任务名称重复")
        }
        return Response.ofSuccess()
    }

    /**
     * 高级配置项按枚举名做校验，保证数据库中只落已知策略值。
     */
    private fun validAdvanced(jobInfo: TaskInfo): Response<String> {
        if (jobInfo.executorRouteStrategy == null) {
            return Response.ofFail("路由策略非法")
        }
        if (jobInfo.misfireStrategy == null) {
            return Response.ofFail("调度过期策略非法")
        }
        if (jobInfo.executorBlockStrategy == null) {
            return Response.ofFail("阻塞处理策略非法")
        }
        return Response.ofSuccess()
    }

    /**
     * 子任务配置需要同时校验格式、存在性和跨任务组权限，并在成功后归一化为无多余逗号的串。
     */
    private fun validAndNormalizeChildTask(
        jobInfo: TaskInfo,
        loginInfo: LoginInfo,
        rejectSelfReference: Boolean
    ): Response<String> {
        if (!StringTool.isNotBlank(jobInfo.childTaskId)) {
            return Response.ofSuccess()
        }

        val childTaskIds = jobInfo.childTaskId!!.split(",")
        val normalizedChildTaskIds = ArrayList<String>()
        for (childTaskIdItem in childTaskIds) {
            if (!StringTool.isNotBlank(childTaskIdItem) || !StringTool.isNumeric(childTaskIdItem)) {
                return Response.ofFail(
                    MessageFormat.format(
                        "子任务ID({0})非法",
                        childTaskIdItem
                    )
                )
            }

            val childTaskId = childTaskIdItem.toInt()
            if (rejectSelfReference && childTaskId == jobInfo.id) {
                return Response.ofFail("子任务ID($childTaskId)非法")
            }

            val childTaskInfo = taskInfoMapper.loadById(childTaskId)
                ?: return Response.ofFail(
                    MessageFormat.format(
                        "子任务ID({0})不存在",
                        childTaskIdItem
                    )
                )
            if (!JobGroupPermissionUtil.hasJobGroupPermission(loginInfo, childTaskInfo.executorId)) {
                return Response.ofFail(
                    MessageFormat.format(
                        "子任务ID({0})权限拦截",
                        childTaskIdItem
                    )
                )
            }
            normalizedChildTaskIds.add(childTaskIdItem)
        }

        jobInfo.childTaskId = normalizedChildTaskIds.joinToString(",")
        return Response.ofSuccess()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotServiceImpl::class.java)
    }
}
