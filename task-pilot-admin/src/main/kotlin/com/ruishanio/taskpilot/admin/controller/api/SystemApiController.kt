package com.ruishanio.taskpilot.admin.controller.api

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.model.dto.TaskPilotBootResourceDTO
import com.ruishanio.taskpilot.admin.scheduler.exception.TaskPilotException
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap

/**
 * 系统级前端启动信息接口。
 *
 * 将登录态、菜单与版本信息统一挂到 `/system/bootstrap`，让前端初始化入口和业务资源接口分层更清晰。
 */
@RestController
@RequestMapping(ManageRoute.API_MANAGE_SYSTEM)
class SystemApiController {
    @Autowired(required = false)
    private var buildProperties: BuildProperties? = null

    @RequestMapping("/bootstrap")
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
            ?: "0.0.0"
    }

    /**
     * 前端菜单只保留核心业务入口，并在普通用户场景按角色做裁剪。
     */
    private fun buildMenuList(loginInfo: LoginInfo): List<TaskPilotBootResourceDTO> {
        var resourceList = listOf(
            TaskPilotBootResourceDTO(1, 0, "工作台", 1, "", "/dashboard", "fa-home", 1, 0, null),
            TaskPilotBootResourceDTO(2, 0, "任务管理", 1, "", "/task_info", "fa-clock-o", 2, 0, null),
            TaskPilotBootResourceDTO(3, 0, "调度日志", 1, "", "/task_log", "fa-database", 3, 0, null),
            TaskPilotBootResourceDTO(4, 0, "执行器管理", 1, Consts.ADMIN_ROLE, "/executor", "fa-cloud", 4, 0, null),
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
}
