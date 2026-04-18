package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.mapper.TaskPilotGroupMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.admin.scheduler.misfire.MisfireStrategyEnum
import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.admin.scheduler.type.ScheduleTypeEnum
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.core.constant.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.ArrayList
import java.util.Date

/**
 * 任务配置管理控制器。
 */
@Controller
@RequestMapping("/jobinfo")
class JobInfoController {
    @Resource
    private lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Resource
    private lateinit var taskPilotService: TaskPilotService

    @RequestMapping
    fun index(
        request: HttpServletRequest,
        model: Model,
        @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") jobGroup: Int
    ): String {
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values())
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values())
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values())
        model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values())
        model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values())

        val jobGroupListTotal = taskPilotGroupMapper.findAll()
        val jobGroupList = JobGroupPermissionUtil.filterJobGroupByPermission(request, jobGroupListTotal)
        if (CollectionTool.isEmpty(jobGroupList)) {
            throw TaskPilotException(I18nUtil.getString("jobgroup_empty"))
        }

        val normalizedJobGroup = if (jobGroupList.map(TaskPilotGroup::id).contains(jobGroup)) jobGroup else -1
        model.addAttribute("JobGroupList", jobGroupList)
        model.addAttribute("jobGroup", normalizedJobGroup)
        return "biz/job.list"
    }

    @RequestMapping("/pageList")
    @ResponseBody
    fun pageList(
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        @RequestParam jobGroup: Int,
        @RequestParam triggerStatus: Int,
        @RequestParam jobDesc: String?,
        @RequestParam executorHandler: String?,
        @RequestParam author: String?
    ): Response<PageModel<TaskPilotInfo>> {
        JobGroupPermissionUtil.validJobGroupPermission(request, jobGroup)
        return taskPilotService.pageList(offset, pagesize, jobGroup, triggerStatus, jobDesc, executorHandler, author)
    }

    @RequestMapping("/insert")
    @ResponseBody
    fun add(request: HttpServletRequest, jobInfo: TaskPilotInfo): Response<String> {
        val loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, jobInfo.jobGroup)
        return taskPilotService.add(jobInfo, loginInfo)
    }

    @RequestMapping("/update")
    @ResponseBody
    fun update(request: HttpServletRequest, jobInfo: TaskPilotInfo): Response<String> {
        val loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, jobInfo.jobGroup)
        return taskPilotService.update(jobInfo, loginInfo)
    }

    @RequestMapping("/delete")
    @ResponseBody
    fun delete(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"))
        }
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        return taskPilotService.remove(ids[0], loginInfo)
    }

    @RequestMapping("/stop")
    @ResponseBody
    fun pause(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"))
        }
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        return taskPilotService.stop(ids[0], loginInfo)
    }

    @RequestMapping("/start")
    @ResponseBody
    fun start(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"))
        }
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        return taskPilotService.start(ids[0], loginInfo)
    }

    @RequestMapping("/trigger")
    @ResponseBody
    fun triggerJob(
        request: HttpServletRequest,
        @RequestParam("id") id: Int,
        @RequestParam("executorParam") executorParam: String?,
        @RequestParam("addressList") addressList: String?
    ): Response<String> {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        return taskPilotService.trigger(loginInfo, id, executorParam, addressList)
    }

    /**
     * 预计算未来 5 次触发时间，供前端保存前验证调度表达式。
     */
    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    fun nextTriggerTime(
        @RequestParam("scheduleType") scheduleType: String?,
        @RequestParam("scheduleConf") scheduleConf: String?
    ): Response<List<String>> {
        if (StringTool.isBlank(scheduleType) || StringTool.isBlank(scheduleConf)) {
            return Response.ofSuccess(ArrayList())
        }

        val paramTaskPilotInfo = TaskPilotInfo().apply {
            this.scheduleType = scheduleType
            this.scheduleConf = scheduleConf
        }

        val result = ArrayList<String>()
        try {
            var lastTime = Date()
            for (i in 0 until 5) {
                val scheduleTypeEnum = ScheduleTypeEnum.match(paramTaskPilotInfo.scheduleType, ScheduleTypeEnum.NONE)
                    ?: ScheduleTypeEnum.NONE
                lastTime = scheduleTypeEnum.scheduleType.generateNextTriggerTime(paramTaskPilotInfo, lastTime) ?: break
                result.add(DateTool.formatDateTime(lastTime))
            }
        } catch (e: Exception) {
            logger.error(">>>>>>>>>>> 计算下次触发时间时发生异常。scheduleType={}, scheduleConf={}", scheduleType, scheduleConf, e)
            return Response.ofFail(
                I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid") + e.message
            )
        }
        return Response.ofSuccess(result)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobInfoController::class.java)
    }
}
