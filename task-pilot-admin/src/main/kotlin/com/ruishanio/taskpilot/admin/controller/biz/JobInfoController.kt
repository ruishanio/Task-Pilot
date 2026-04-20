package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.constant.TriggerStatus
import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.admin.scheduler.type.toScheduleType
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

/**
 * 任务配置管理控制器。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_TASK_INFO)
class JobInfoController {
    @Resource
    private lateinit var taskPilotService: TaskPilotService

    @Resource
    private lateinit var executorMapper: ExecutorMapper

    /**
     * 任务管理页依赖的筛选项与枚举元数据直接挂在资源控制器下，避免继续分散到独立 frontend 前缀。
     */
    @RequestMapping("/meta")
    @ResponseBody
    fun meta(
        request: HttpServletRequest,
        @RequestParam(value = "executorId", required = false, defaultValue = "-1") executorId: Int
    ): Response<Map<String, Any>> {
        val jobGroupList = JobGroupPermissionUtil.filterJobGroupByPermission(request, executorMapper.findAll())
        if (CollectionTool.isEmpty(jobGroupList)) {
            throw TaskPilotException("不存在有效执行器,请联系管理员")
        }

        val accessibleGroupIds = jobGroupList.map(Executor::id)
        val selectedExecutorId = if (accessibleGroupIds.contains(executorId)) executorId else jobGroupList[0].id

        val data = HashMap<String, Any>()
        data["groups"] = jobGroupList
        data["selectedExecutorId"] = selectedExecutorId
        data["triggerStatusOptions"] = listOf(
            option("-1", "全部"),
            option(TriggerStatus.STOPPED.value.toString(), "停止"),
            option(TriggerStatus.RUNNING.value.toString(), "启动")
        )
        data["scheduleTypeOptions"] = ScheduleTypeEnum.entries.map { option(it.name, it.title) }
        data["glueTypeOptions"] = GlueTypeEnum.entries.map {
            option(it.name, it.desc).also { payload ->
                payload["isScript"] = it.isScript
                payload["cmd"] = it.cmd
                payload["suffix"] = it.suffix
            }
        }
        data["executorRouteStrategyOptions"] = ExecutorRouteStrategyEnum.entries.map { option(it.name, it.title) }
        data["executorBlockStrategyOptions"] = ExecutorBlockStrategyEnum.entries.map { option(it.name, it.title) }
        data["misfireStrategyOptions"] = MisfireStrategyEnum.entries.map { option(it.name, it.title) }
        return Response.ofSuccess(data)
    }

    @RequestMapping("/pageList")
    @ResponseBody
    fun pageList(
        request: HttpServletRequest,
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        @RequestParam executorId: Int,
        @RequestParam triggerStatus: Int,
        @RequestParam(required = false) taskName: String?,
        @RequestParam taskDesc: String?,
        @RequestParam executorHandler: String?,
        @RequestParam author: String?
    ): Response<PageModel<TaskInfo>> {
        JobGroupPermissionUtil.validJobGroupPermission(request, executorId)
        return taskPilotService.pageList(offset, pagesize, executorId, triggerStatus, taskName, taskDesc, executorHandler, author)
    }

    @RequestMapping("/insert")
    @ResponseBody
    fun add(request: HttpServletRequest, taskInfo: TaskInfo): Response<String> {
        val loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, taskInfo.executorId)
        return taskPilotService.add(taskInfo, loginInfo)
    }

    @RequestMapping("/update")
    @ResponseBody
    fun update(request: HttpServletRequest, taskInfo: TaskInfo): Response<String> {
        val loginInfo = JobGroupPermissionUtil.validJobGroupPermission(request, taskInfo.executorId)
        return taskPilotService.update(taskInfo, loginInfo)
    }

    @RequestMapping("/delete")
    @ResponseBody
    fun delete(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail("请选择一条数据")
        }
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        return taskPilotService.remove(ids[0], loginInfo)
    }

    @RequestMapping("/stop")
    @ResponseBody
    fun pause(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail("请选择一条数据")
        }
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("not login.")
        return taskPilotService.stop(ids[0], loginInfo)
    }

    @RequestMapping("/start")
    @ResponseBody
    fun start(request: HttpServletRequest, @RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail("请选择一条数据")
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
        @RequestParam("scheduleType") scheduleType: ScheduleTypeEnum?,
        @RequestParam("scheduleConf") scheduleConf: String?
    ): Response<List<String>> {
        if (scheduleType == null || scheduleConf.isNullOrBlank()) {
            return Response.ofSuccess(ArrayList())
        }

        val paramTaskInfo = TaskInfo().apply {
            this.scheduleType = scheduleType
            this.scheduleConf = scheduleConf
        }

        val result = ArrayList<String>()
        try {
            var lastTime = Date()
            for (i in 0 until 5) {
                lastTime = scheduleType.toScheduleType().generateNextTriggerTime(paramTaskInfo, lastTime) ?: break
                result.add(DateTool.formatDateTime(lastTime))
            }
        } catch (e: Exception) {
            logger.error(">>>>>>>>>>> 计算下次触发时间时发生异常。scheduleType={}, scheduleConf={}", scheduleType, scheduleConf, e)
            return Response.ofFail("调度类型非法${e.message}")
        }
        return Response.ofSuccess(result)
    }

    /**
     * 简单的 `value/label` 结构沿用历史返回格式，降低前端切路由时的联动成本。
     */
    private fun option(value: String, label: String): MutableMap<String, Any?> {
        val payload = HashMap<String, Any?>()
        payload["value"] = value
        payload["label"] = label
        return payload
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobInfoController::class.java)
    }
}
