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
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.admin.util.JobGroupPermissionUtil
import com.ruishanio.taskpilot.core.constant.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
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
@RequestMapping("/api/frontend")
class FrontendApiController {
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
        data["appName"] = I18nUtil.getString("admin_name")
        data["appNameFull"] = I18nUtil.getString("admin_name_full")
        data["version"] = I18nUtil.getString("admin_version")
        data["menus"] = buildMenuList(loginInfo)
        data["user"] = buildUserInfo(loginInfo)
        return Response.ofSuccess(data)
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
            throw TaskPilotException(I18nUtil.getString("jobgroup_empty"))
        }

        val accessibleGroupIds = jobGroupList.map(TaskPilotGroup::id)
        val selectedJobGroup = if (accessibleGroupIds.contains(jobGroup)) jobGroup else jobGroupList[0].id

        val data = HashMap<String, Any>()
        data["groups"] = jobGroupList
        data["selectedJobGroup"] = selectedJobGroup
        data["triggerStatusOptions"] = listOf(
            option("-1", I18nUtil.getString("system_all")),
            option(TriggerStatus.STOPPED.value.toString(), I18nUtil.getString("jobinfo_opt_stop")),
            option(TriggerStatus.RUNNING.value.toString(), I18nUtil.getString("jobinfo_opt_start"))
        )
        data["scheduleTypeOptions"] = ScheduleTypeEnum.values().map { option(it.name, it.title) }
        data["glueTypeOptions"] = GlueTypeEnum.values().map {
            option(it.name, it.desc).also { payload ->
                payload["isScript"] = it.isScript
                payload["cmd"] = it.cmd
                payload["suffix"] = it.suffix
            }
        }
        data["executorRouteStrategyOptions"] = ExecutorRouteStrategyEnum.values().map { option(it.name, it.title) }
        data["executorBlockStrategyOptions"] = ExecutorBlockStrategyEnum.values().map { option(it.name, it.title) }
        data["misfireStrategyOptions"] = MisfireStrategyEnum.values().map { option(it.name, it.title) }
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
            throw TaskPilotException(I18nUtil.getString("jobgroup_empty"))
        }

        var selectedGroupParam = jobGroup ?: 0
        if (jobId != null && jobId > 0) {
            val jobInfo = taskPilotInfoMapper.loadById(jobId)
                ?: throw RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"))
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
            option("-1", I18nUtil.getString("joblog_status_all")),
            option("1", I18nUtil.getString("joblog_status_suc")),
            option("2", I18nUtil.getString("joblog_status_fail")),
            option("3", I18nUtil.getString("joblog_status_running"))
        )
        data["clearLogOptions"] = listOf(
            option("1", I18nUtil.getString("joblog_clean_type_1")),
            option("2", I18nUtil.getString("joblog_clean_type_2")),
            option("3", I18nUtil.getString("joblog_clean_type_3")),
            option("4", I18nUtil.getString("joblog_clean_type_4")),
            option("5", I18nUtil.getString("joblog_clean_type_5")),
            option("6", I18nUtil.getString("joblog_clean_type_6")),
            option("7", I18nUtil.getString("joblog_clean_type_7")),
            option("8", I18nUtil.getString("joblog_clean_type_8")),
            option("9", I18nUtil.getString("joblog_clean_type_9"))
        )
        return Response.ofSuccess(data)
    }

    @RequestMapping("/user/meta")
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun userMeta(): Response<Map<String, Any>> {
        val data = HashMap<String, Any>()
        data["groups"] = taskPilotGroupMapper.findAll()
        data["roleOptions"] = listOf(
            option("1", I18nUtil.getString("user_role_admin")),
            option("0", I18nUtil.getString("user_role_normal"))
        )
        return Response.ofSuccess(data)
    }

    /**
     * 前端菜单只保留核心业务入口，并在普通用户场景按角色做裁剪。
     */
    private fun buildMenuList(loginInfo: LoginInfo): List<TaskPilotBootResourceDTO> {
        var resourceList = listOf(
            TaskPilotBootResourceDTO(1, 0, I18nUtil.getString("job_dashboard_name"), 1, "", "/dashboard", "fa-home", 1, 0, null),
            TaskPilotBootResourceDTO(2, 0, I18nUtil.getString("jobinfo_name"), 1, "", "/jobinfo", "fa-clock-o", 2, 0, null),
            TaskPilotBootResourceDTO(3, 0, I18nUtil.getString("joblog_name"), 1, "", "/joblog", "fa-database", 3, 0, null),
            TaskPilotBootResourceDTO(4, 0, I18nUtil.getString("jobgroup_name"), 1, Consts.ADMIN_ROLE, "/jobgroup", "fa-cloud", 4, 0, null),
            TaskPilotBootResourceDTO(5, 0, I18nUtil.getString("user_manage"), 1, Consts.ADMIN_ROLE, "/user", "fa-users", 5, 0, null)
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
