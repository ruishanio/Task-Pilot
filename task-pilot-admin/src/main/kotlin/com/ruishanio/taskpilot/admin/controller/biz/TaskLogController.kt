package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskLogMapper
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.model.TaskLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.admin.util.ExecutorPermissionUtil
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.util.HtmlUtils
import java.util.Date
import java.util.HashMap

/**
 * 任务日志控制器。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_TASK_LOG)
class TaskLogController {
    @Resource
    lateinit var executorMapper: ExecutorMapper

    @Resource
    lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    lateinit var taskLogMapper: TaskLogMapper

    /**
     * 日志页的执行器、任务与筛选项配置收口到日志资源控制器，和页面主查询路径保持同一前缀。
     */
    @RequestMapping("/meta")
    @ResponseBody
    fun meta(
        request: HttpServletRequest,
        @RequestParam(value = "executorId", required = false, defaultValue = "0") executorId: Int?,
        @RequestParam(value = "taskId", required = false, defaultValue = "0") taskId: Int?
    ): Response<Map<String, Any>> {
        val executorList = ExecutorPermissionUtil.filterExecutorByPermission(request, executorMapper.findAll())
        if (CollectionTool.isEmpty(executorList)) {
            throw TaskPilotException("不存在有效执行器,请联系管理员")
        }

        var selectedExecutorParam = executorId ?: 0
        if (taskId != null && taskId > 0) {
            val taskInfo = taskInfoMapper.loadById(taskId)
                ?: throw RuntimeException("任务ID非法")
            selectedExecutorParam = taskInfo.executorId
        }

        val accessibleExecutorIds = executorList.map(Executor::id)
        val selectedExecutorId = if (accessibleExecutorIds.contains(selectedExecutorParam)) {
            selectedExecutorParam
        } else {
            executorList[0].id
        }

        val taskInfoList = taskInfoMapper.getTasksByExecutorId(selectedExecutorId)
        var selectedTaskId = 0
        if (CollectionTool.isNotEmpty(taskInfoList)) {
            val accessibleTaskIds = taskInfoList.map(TaskInfo::id)
            selectedTaskId = if (taskId != null && accessibleTaskIds.contains(taskId)) taskId else taskInfoList[0].id
        }

        val data = HashMap<String, Any>()
        data["executors"] = executorList
        data["tasks"] = taskInfoList
        data["selectedExecutorId"] = selectedExecutorId
        data["selectedTaskId"] = selectedTaskId
        data["logStatusOptions"] = listOf(
            option("-1", "全部"),
            option("1", "成功"),
            option("2", "失败"),
            option("3", "进行中")
        )
        data["clearLogOptions"] = listOf(
            option("1", "清理一个月之前日志数据"),
            option("2", "清理三个月之前日志数据"),
            option("3", "清理六个月之前日志数据"),
            option("4", "清理一年之前日志数据"),
            option("5", "清理一千条以前日志数据"),
            option("6", "清理一万条以前日志数据"),
            option("7", "清理三万条以前日志数据"),
            option("8", "清理十万条以前日志数据"),
            option("9", "清理所有日志数据")
        )
        return Response.ofSuccess(data)
    }

    @RequestMapping("/page")
    @ResponseBody
    fun page(
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        @RequestParam executorId: Int,
        @RequestParam taskId: Int,
        @RequestParam logStatus: Int,
        @RequestParam filterTime: String?
    ): Response<PageModel<TaskLog>> {
        ExecutorPermissionUtil.validExecutorPermission(request, executorId)
        if (taskId < 1) {
            return Response.ofFail("请选择任务")
        }

        var triggerTimeStart: Date? = null
        var triggerTimeEnd: Date? = null
        if (StringTool.isNotBlank(filterTime)) {
            val temp = filterTime!!.split(" - ")
            if (temp.size == 2) {
                triggerTimeStart = DateTool.parseDateTime(temp[0])
                triggerTimeEnd = DateTool.parseDateTime(temp[1])
            }
        }

        val list = taskLogMapper.pageList(offset, pagesize, executorId, taskId, triggerTimeStart, triggerTimeEnd, logStatus)
        val listCount = taskLogMapper.pageListCount(offset, pagesize, executorId, taskId, triggerTimeStart, triggerTimeEnd, logStatus)
        val pageModel = PageModel<TaskLog>()
        pageModel.data = list
        pageModel.total = listCount
        return Response.ofSuccess(pageModel)
    }

    /**
     * 日志正文允许少量 HTML 标记，其余内容仍做转义，避免 XSS。
     */
    private fun filter(originData: String): String {
        val excludeTagMap = HashMap<String, String>()
        excludeTagMap["<br>"] = "###TAG_BR###"
        excludeTagMap["<b>"] = "###TAG_BOLD###"
        excludeTagMap["</b>"] = "###TAG_BOLD_END###"

        var normalized = originData
        for ((key, value) in excludeTagMap) {
            normalized = normalized.replace(key, value)
        }
        normalized = HtmlUtils.htmlEscape(normalized, "UTF-8")
        for ((key, value) in excludeTagMap) {
            normalized = normalized.replace(value, key)
        }
        return normalized
    }

    @RequestMapping("/log_kill")
    @ResponseBody
    fun logKill(request: HttpServletRequest, @RequestParam("id") id: Long): Response<String> {
        val taskLog = taskLogMapper.load(id) ?: return Response.ofFail("日志ID非法")
        val taskInfo = taskInfoMapper.loadById(taskLog.taskId)
            ?: return Response.ofFail("任务ID非法")
        if (TaskPilotContext.HANDLE_CODE_SUCCESS != taskLog.triggerCode) {
            return Response.ofFail("调度失败，无法终止日志")
        }

        ExecutorPermissionUtil.validExecutorPermission(request, taskInfo.executorId)

        val runResult = try {
            val executorBiz: ExecutorBiz = TaskPilotAdminBootstrap.getExecutorBiz(taskLog.executorAddress)!!
            executorBiz.kill(KillRequest(taskInfo.id))
        } catch (e: Exception) {
            logger.error("终止任务执行时发生异常。logId={}", id, e)
            Response.ofFail<String>(e.message)
        }

        return if (TaskPilotContext.HANDLE_CODE_SUCCESS == runResult.code) {
            taskLog.handleCode = TaskPilotContext.HANDLE_CODE_FAIL
            taskLog.handleMsg = "人为操作，主动终止:" + (runResult.msg ?: "")
            taskLog.handleTime = Date()
            TaskPilotAdminBootstrap.instance.taskCompleter.complete(taskLog)
            Response.ofSuccess(runResult.msg)
        } else {
            Response.ofFail(runResult.msg)
        }
    }

    @RequestMapping("/clear_log")
    @ResponseBody
    fun clearLog(
        request: HttpServletRequest,
        @RequestParam("executorId") executorId: Int,
        @RequestParam("taskId") taskId: Int,
        @RequestParam("type") type: Int
    ): Response<String> {
        ExecutorPermissionUtil.validExecutorPermission(request, executorId)
        if (taskId < 1) {
            return Response.ofFail("请选择任务")
        }

        var clearBeforeTime: Date? = null
        var clearBeforeNum = 0
        when (type) {
            1 -> clearBeforeTime = DateTool.addMonths(Date(), -1)
            2 -> clearBeforeTime = DateTool.addMonths(Date(), -3)
            3 -> clearBeforeTime = DateTool.addMonths(Date(), -6)
            4 -> clearBeforeTime = DateTool.addYears(Date(), -1)
            5 -> clearBeforeNum = 1000
            6 -> clearBeforeNum = 10000
            7 -> clearBeforeNum = 30000
            8 -> clearBeforeNum = 100000
            9 -> clearBeforeNum = 0
            else -> return Response.ofFail("清理类型参数异常")
        }

        var logIds: List<Long>
        do {
            logIds = taskLogMapper.findClearLogIds(executorId, taskId, clearBeforeTime, clearBeforeNum, 1000)
            if (logIds.isNotEmpty()) {
                taskLogMapper.clearLog(logIds)
            }
        } while (logIds.isNotEmpty())

        return Response.ofSuccess()
    }

    /**
     * 读取执行器日志时在管理端做一层 XSS 过滤，并在任务已结束后补齐 end 标记。
     */
    @RequestMapping("/log_detail_cat")
    @ResponseBody
    fun logDetailCat(
        @RequestParam("logId") logId: Long,
        @RequestParam("fromLineNum") fromLineNum: Int
    ): Response<LogResult> {
        return try {
            val taskLog = taskLogMapper.load(logId)
                ?: return Response.ofFail("日志ID非法")
            val executorBiz: ExecutorBiz = TaskPilotAdminBootstrap.getExecutorBiz(taskLog.executorAddress)!!
            val logResult = executorBiz.log(LogRequest(taskLog.triggerTime!!.time, logId, fromLineNum))
            val logData = logResult.data

            if (logData != null && logData.fromLineNum > logData.toLineNum && taskLog.handleCode > 0) {
                logData.isEnd = true
            }
            if (logData != null && StringTool.isNotBlank(logData.logContent)) {
                logData.logContent = filter(logData.logContent!!)
            }
            logResult
        } catch (e: Exception) {
            logger.error("读取任务日志详情时发生异常。logId={}", logId, e)
            Response.ofFail(e.message)
        }
    }

    /**
     * 继续复用轻量 `value/label` 结构，避免日志页筛选器额外引入 DTO。
     */
    private fun option(value: String, label: String): MutableMap<String, Any?> {
        val payload = HashMap<String, Any?>()
        payload["value"] = value
        payload["label"] = label
        return payload
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskLogController::class.java)
    }
}
