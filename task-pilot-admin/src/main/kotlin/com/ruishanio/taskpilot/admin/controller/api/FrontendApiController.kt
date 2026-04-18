package com.ruishanio.taskpilot.admin.controller.api

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.constant.TriggerStatus
import com.ruishanio.taskpilot.admin.mapper.TaskPilotGroupMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.model.dto.TaskPilotBootResourceDTO
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.admin.scheduler.misfire.MisfireStrategyEnum
import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.admin.scheduler.type.ScheduleTypeEnum
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.core.constant.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap

/**
 * 前后端分离场景下的首屏与元数据接口。
 */
@RestController
@RequestMapping(ManageRoute.API_MANAGE_FRONTEND)
class FrontendApiController {
    @Autowired(required = false)
    private var buildProperties: BuildProperties? = null

    @Resource
    private lateinit var taskPilotService: TaskPilotService

    @Resource
    private lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Resource
    private lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @RequestMapping("/bootstrap")
    @TaskPilotAuth
    fun bootstrap(request: HttpServletRequest): Response<Map<String, Any>> {
        val loginInfo = requireLoginInfo(request)
        val data = HashMap<String, Any>()
        data["appName"] = "Task Pilot"
        data["appNameFull"] = "Task Pilot｜分布式任务调度平台"
        data["version"] = resolveAppVersion()
        data["menus"] = buildMenuList(loginInfo)
        data["user"] = buildUserInfo(loginInfo)
        return Response.ofSuccess(data)
    }

    /**
     * 优先使用构建期注入的版本元信息，避免接口展示版本与 Maven 实际发布版本脱节。
     */
    private fun resolveAppVersion(): String {
        return buildProperties?.version
            ?: javaClass.`package`?.implementationVersion
            ?: "1.0.0"
    }

    /**
     * Dashboard 统计直接复用服务层聚合结果，避免前后端接口分叉出两套统计口径。
     */
    @RequestMapping("/dashboard")
    @TaskPilotAuth
    fun dashboard(): Response<Map<String, Any>> = Response.ofSuccess(taskPilotService.dashboardInfo())

    @RequestMapping("/jobinfo/meta")
    @TaskPilotAuth
    fun jobInfoMeta(
        request: HttpServletRequest,
        @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") jobGroup: Int
    ): Response<Map<String, Any>> {
        val jobGroupList = JobGroupPermissionUtil.filterJobGroupByPermission(request, taskPilotGroupMapper.findAll())
        if (CollectionTool.isEmpty(jobGroupList)) {
            throw TaskPilotException("不存在有效执行器,请联系管理员")
        }

        val accessibleGroupIds = jobGroupList.map(TaskPilotGroup::id)
        val selectedJobGroup = if (accessibleGroupIds.contains(jobGroup)) jobGroup else jobGroupList[0].id

        val data = HashMap<String, Any>()
        data["groups"] = jobGroupList
        data["selectedJobGroup"] = selectedJobGroup
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

    @RequestMapping("/joblog/meta")
    @TaskPilotAuth
    fun jobLogMeta(
        request: HttpServletRequest,
        @RequestParam(value = "jobGroup", required = false, defaultValue = "0") jobGroup: Int?,
        @RequestParam(value = "jobId", required = false, defaultValue = "0") jobId: Int?
    ): Response<Map<String, Any>> {
        val jobGroupList = JobGroupPermissionUtil.filterJobGroupByPermission(request, taskPilotGroupMapper.findAll())
        if (CollectionTool.isEmpty(jobGroupList)) {
            throw TaskPilotException("不存在有效执行器,请联系管理员")
        }

        var selectedGroupParam = jobGroup ?: 0
        if (jobId != null && jobId > 0) {
            val jobInfo = taskPilotInfoMapper.loadById(jobId)
                ?: throw RuntimeException("任务ID非法")
            selectedGroupParam = jobInfo.jobGroup
        }

        val accessibleGroupIds = jobGroupList.map(TaskPilotGroup::id)
        val selectedJobGroup = if (accessibleGroupIds.contains(selectedGroupParam)) selectedGroupParam else jobGroupList[0].id

        val jobInfoList = taskPilotInfoMapper.getJobsByGroup(selectedJobGroup)
        var selectedJobId = 0
        if (CollectionTool.isNotEmpty(jobInfoList)) {
            val accessibleJobIds = jobInfoList.map(TaskPilotInfo::id)
            selectedJobId = if (jobId != null && accessibleJobIds.contains(jobId)) jobId else jobInfoList[0].id
        }

        val data = HashMap<String, Any>()
        data["groups"] = jobGroupList
        data["jobs"] = jobInfoList
        data["selectedJobGroup"] = selectedJobGroup
        data["selectedJobId"] = selectedJobId
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

    @RequestMapping("/user/meta")
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun userMeta(): Response<Map<String, Any>> {
        val data = HashMap<String, Any>()
        data["groups"] = taskPilotGroupMapper.findAll()
        data["roleOptions"] = listOf(
            option("1", "管理员"),
            option("0", "普通用户")
        )
        return Response.ofSuccess(data)
    }

    /**
     * 前端菜单只保留核心业务入口，并在普通用户场景按角色做裁剪。
     */
    private fun buildMenuList(loginInfo: LoginInfo): List<TaskPilotBootResourceDTO> {
        var resourceList = listOf(
            TaskPilotBootResourceDTO(1, 0, "工作台", 1, "", "/dashboard", "fa-home", 1, 0, null),
            TaskPilotBootResourceDTO(2, 0, "任务管理", 1, "", "/jobinfo", "fa-clock-o", 2, 0, null),
            TaskPilotBootResourceDTO(3, 0, "调度日志", 1, "", "/joblog", "fa-database", 3, 0, null),
            TaskPilotBootResourceDTO(4, 0, "执行器管理", 1, Consts.ADMIN_ROLE, "/jobgroup", "fa-cloud", 4, 0, null),
            TaskPilotBootResourceDTO(5, 0, "用户管理", 1, Consts.ADMIN_ROLE, "/user", "fa-users", 5, 0, null)
        )
        if (!TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess) {
            resourceList = resourceList.filter { StringTool.isBlank(it.permission) }
        }
        return resourceList.sortedWith(Comparator.comparing(TaskPilotBootResourceDTO::order))
            .toCollection(ArrayList())
    }

    /**
     * 对外暴露经过整理的登录态，避免前端依赖 SSO 内部结构。
     */
    private fun buildUserInfo(loginInfo: LoginInfo): Map<String, Any?> {
        val data = HashMap<String, Any?>()
        data["userId"] = loginInfo.userId
        data["userName"] = loginInfo.userName
        data["roleList"] = loginInfo.roleList
        data["permissionList"] = loginInfo.permissionList
        data["extraInfo"] = loginInfo.extraInfo
        data["isAdmin"] = TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess
        return data
    }

    /**
     * 当前控制器所有接口都需要登录态，这里统一收口读取与兜底异常。
     */
    private fun requireLoginInfo(request: HttpServletRequest): LoginInfo {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data
        if (!loginInfoResponse.isSuccess || loginInfo == null) {
            throw TaskPilotException("login info not found")
        }
        return loginInfo
    }

    /**
     * 简单的 `value/label` 选项结构，避免额外 DTO 扩散。
     */
    private fun option(value: String, label: String): MutableMap<String, Any?> {
        val payload = HashMap<String, Any?>()
        payload["value"] = value
        payload["label"] = label
        return payload
    }
}
