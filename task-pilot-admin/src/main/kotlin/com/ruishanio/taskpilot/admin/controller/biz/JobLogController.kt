package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.mapper.TaskPilotGroupMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotLogMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.model.TaskPilotLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
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
@RequestMapping("/joblog")
class JobLogController {
    @Resource
    private lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Resource
    lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Resource
    lateinit var taskPilotLogMapper: TaskPilotLogMapper

    @Autowired
    private lateinit var taskPilotService: TaskPilotService

    @RequestMapping
    fun index(
        request: HttpServletRequest,
        model: Model,
        @RequestParam(value = "jobGroup", required = false, defaultValue = "0") jobGroup: Int?,
        @RequestParam(value = "jobId", required = false, defaultValue = "0") jobId: Int?
    ): String {
        val jobGroupListTotal = taskPilotGroupMapper.findAll()
        val jobGroupList = JobGroupPermissionUtil.filterJobGroupByPermission(request, jobGroupListTotal)
        if (CollectionTool.isEmpty(jobGroupList)) {
            throw TaskPilotException(I18nUtil.getString("jobgroup_empty"))
        }

        var selectedJobGroup = jobGroup ?: 0
        var selectedJobId = jobId ?: 0
        if (selectedJobId > 0) {
            val jobInfo = taskPilotInfoMapper.loadById(selectedJobId)
                ?: throw RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"))
            selectedJobGroup = jobInfo.jobGroup
        } else if (selectedJobGroup > 0) {
            if (jobGroupListTotal.none { it.id == selectedJobGroup }) {
                selectedJobGroup = jobGroupList[0].id
            }
            selectedJobId = 0
        } else {
            selectedJobGroup = jobGroupList[0].id
            selectedJobId = 0
        }

        val jobInfoList = taskPilotInfoMapper.getJobsByGroup(selectedJobGroup)
        if (CollectionTool.isEmpty(jobInfoList)) {
            selectedJobId = 0
        } else if (!jobInfoList.map(TaskPilotInfo::id).contains(selectedJobId)) {
            selectedJobId = jobInfoList[0].id
        }

        model.addAttribute("JobGroupList", jobGroupList)
        model.addAttribute("jobInfoList", jobInfoList)
        model.addAttribute("jobGroup", selectedJobGroup)
        model.addAttribute("jobId", selectedJobId)
        return "biz/log.list"
    }

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
    ): Response<PageModel<TaskPilotLog>> {
        JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup)
        if (jobId < 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_job"))
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

        val list = taskPilotLogMapper.pageList(offset, pagesize, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus)
        val listCount = taskPilotLogMapper.pageListCount(offset, pagesize, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus)
        val pageModel = PageModel<TaskPilotLog>()
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
        val log = taskPilotLogMapper.load(id) ?: return Response.ofFail(I18nUtil.getString("joblog_logid_unvalid"))
        val jobInfo = taskPilotInfoMapper.loadById(log.jobId)
            ?: return Response.ofFail(I18nUtil.getString("jobinfo_glue_jobid_unvalid"))
        if (TaskPilotContext.HANDLE_CODE_SUCCESS != log.triggerCode) {
            return Response.ofFail(I18nUtil.getString("joblog_kill_log_limit"))
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
            log.handleMsg = I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.msg ?: "")
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
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_job"))
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
            else -> return Response.ofFail(I18nUtil.getString("joblog_clean_type_unvalid"))
        }

        var logIds: List<Long>
        do {
            logIds = taskPilotLogMapper.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000)
            if (logIds.isNotEmpty()) {
                taskPilotLogMapper.clearLog(logIds)
            }
        } while (logIds.isNotEmpty())

        return Response.ofSuccess()
    }

    @RequestMapping("/logDetailPage")
    fun logDetailPage(request: HttpServletRequest, @RequestParam("id") id: Long, model: Model): String {
        val jobLog = taskPilotLogMapper.load(id) ?: throw RuntimeException(I18nUtil.getString("joblog_logid_unvalid"))
        JobGroupPermissionUtil.validJobGroupPermission(request, jobLog.jobGroup)
        val jobInfo = taskPilotInfoMapper.loadById(jobLog.jobId)

        model.addAttribute("triggerCode", jobLog.triggerCode)
        model.addAttribute("handleCode", jobLog.handleCode)
        model.addAttribute("logId", jobLog.id)
        model.addAttribute("jobInfo", jobInfo)
        return "biz/log.detail"
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
            val jobLog = taskPilotLogMapper.load(logId)
                ?: return Response.ofFail(I18nUtil.getString("joblog_logid_unvalid"))
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
