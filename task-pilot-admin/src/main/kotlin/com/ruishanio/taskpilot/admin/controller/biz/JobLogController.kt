package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskLogMapper
import com.ruishanio.taskpilot.admin.model.TaskLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
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
@RequestMapping(ManageRoute.API_MANAGE_JOBLOG)
class JobLogController {
    @Resource
    lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    lateinit var taskLogMapper: TaskLogMapper

    @RequestMapping("/pageList")
    @ResponseBody
    fun pageList(
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        @RequestParam jobGroup: Int,
        @RequestParam jobId: Int,
        @RequestParam logStatus: Int,
        @RequestParam filterTime: String?
    ): Response<PageModel<TaskLog>> {
        JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup)
        if (jobId < 1) {
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

        val list = taskLogMapper.pageList(offset, pagesize, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus)
        val listCount = taskLogMapper.pageListCount(offset, pagesize, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus)
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

    @RequestMapping("/logKill")
    @ResponseBody
    fun logKill(request: HttpServletRequest, @RequestParam("id") id: Long): Response<String> {
        val log = taskLogMapper.load(id) ?: return Response.ofFail("日志ID非法")
        val jobInfo = taskInfoMapper.loadById(log.jobId)
            ?: return Response.ofFail("任务ID非法")
        if (TaskPilotContext.HANDLE_CODE_SUCCESS != log.triggerCode) {
            return Response.ofFail("调度失败，无法终止日志")
        }

        JobGroupPermissionUtil.validJobGroupPermission(request, jobInfo.jobGroup)

        val runResult = try {
            val executorBiz: ExecutorBiz = TaskPilotAdminBootstrap.getExecutorBiz(log.executorAddress)!!
            executorBiz.kill(KillRequest(jobInfo.id))
        } catch (e: Exception) {
            logger.error("终止任务执行时发生异常。logId={}", id, e)
            Response.ofFail<String>(e.message)
        }

        return if (TaskPilotContext.HANDLE_CODE_SUCCESS == runResult.code) {
            log.handleCode = TaskPilotContext.HANDLE_CODE_FAIL
            log.handleMsg = "人为操作，主动终止:" + (runResult.msg ?: "")
            log.handleTime = Date()
            TaskPilotAdminBootstrap.instance.jobCompleter.complete(log)
            Response.ofSuccess(runResult.msg)
        } else {
            Response.ofFail(runResult.msg)
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    fun clearLog(
        request: HttpServletRequest,
        @RequestParam("jobGroup") jobGroup: Int,
        @RequestParam("jobId") jobId: Int,
        @RequestParam("type") type: Int
    ): Response<String> {
        JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup)
        if (jobId < 1) {
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
            logIds = taskLogMapper.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000)
            if (logIds.isNotEmpty()) {
                taskLogMapper.clearLog(logIds)
            }
        } while (logIds.isNotEmpty())

        return Response.ofSuccess()
    }

    /**
     * 读取执行器日志时在管理端做一层 XSS 过滤，并在任务已结束后补齐 end 标记。
     */
    @RequestMapping("/logDetailCat")
    @ResponseBody
    fun logDetailCat(
        @RequestParam("logId") logId: Long,
        @RequestParam("fromLineNum") fromLineNum: Int
    ): Response<LogResult> {
        return try {
            val jobLog = taskLogMapper.load(logId)
                ?: return Response.ofFail("日志ID非法")
            val executorBiz: ExecutorBiz = TaskPilotAdminBootstrap.getExecutorBiz(jobLog.executorAddress)!!
            val logResult = executorBiz.log(LogRequest(jobLog.triggerTime!!.time, logId, fromLineNum))
            val logData = logResult.data

            if (logData != null && logData.fromLineNum > logData.toLineNum && jobLog.handleCode > 0) {
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

    companion object {
        private val logger = LoggerFactory.getLogger(JobLogController::class.java)
    }
}
