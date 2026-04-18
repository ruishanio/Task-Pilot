package com.ruishanio.taskpilot.admin.util

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.tool.core.StringTool
import jakarta.servlet.http.HttpServletRequest

/**
 * 任务组权限工具。
 *
 * 统一收口任务组权限判断，避免 controller 与 service 各自复制权限逻辑。
 */
object JobGroupPermissionUtil {
    /**
     * 管理员默认拥有全部任务组权限，普通用户则从扩展信息中的 jobGroups 字段判断。
     */
    fun hasJobGroupPermission(loginInfo: LoginInfo, jobGroup: Int): Boolean {
        if (TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess) {
            return true
        }
        val jobGroups: List<String> = if (loginInfo.extraInfo != null && loginInfo.extraInfo!!.containsKey("jobGroups")) {
            StringTool.split(loginInfo.extraInfo!!["jobGroups"], ",")
        } else {
            ArrayList()
        } ?: emptyList()
        return jobGroups.contains(jobGroup.toString())
    }

    /**
     * 权限校验失败时直接抛错，让上层沿用统一异常处理逻辑。
     */
    fun validJobGroupPermission(request: HttpServletRequest, jobGroup: Int): LoginInfo {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data
        if (!(loginInfoResponse.isSuccess && loginInfo != null && hasJobGroupPermission(loginInfo, jobGroup))) {
            throw RuntimeException(I18nUtil.getString("system_permission_limit") + "[username=${loginInfo?.userName}]")
        }
        return loginInfo
    }

    /**
     * 普通用户只能看到被授予的任务组，管理员保持原列表不变。
     */
    fun filterJobGroupByPermission(request: HttpServletRequest, jobGroupListTotal: List<TaskPilotGroup>): List<TaskPilotGroup> {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return ArrayList()
        if (TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess) {
            return jobGroupListTotal
        }

        val extraInfo = loginInfo.extraInfo
        val jobGroups: List<String> = if (extraInfo != null && extraInfo["jobGroups"] != null) {
            StringTool.split(extraInfo["jobGroups"], ",")
        } else {
            ArrayList()
        } ?: emptyList()
        return jobGroupListTotal.filter { jobGroups.contains(it.id.toString()) }
    }
}
