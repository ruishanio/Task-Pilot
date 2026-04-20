package com.ruishanio.taskpilot.admin.util

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.tool.core.StringTool
import jakarta.servlet.http.HttpServletRequest

/**
 * 任务组权限工具。
 *
 * 统一收口任务组权限判断，避免 controller 与 service 各自复制权限逻辑。
 */
object ExecutorPermissionUtil {
    /**
     * 管理员默认拥有全部执行器权限，普通用户则从扩展信息中的 executorIds 字段判断。
     */
    fun hasExecutorPermission(loginInfo: LoginInfo, executorId: Int): Boolean {
        if (TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess) {
            return true
        }
        val executorIds: List<String> = if (loginInfo.extraInfo != null && loginInfo.extraInfo!!.containsKey("executorIds")) {
            StringTool.split(loginInfo.extraInfo!!["executorIds"], ",")
        } else {
            ArrayList()
        } ?: emptyList()
        return executorIds.contains(executorId.toString())
    }

    /**
     * 权限校验失败时直接抛错，让上层沿用统一异常处理逻辑。
     */
    fun validExecutorPermission(request: HttpServletRequest, executorId: Int): LoginInfo {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data
        if (!(loginInfoResponse.isSuccess && loginInfo != null && hasExecutorPermission(loginInfo, executorId))) {
            throw RuntimeException("权限拦截[username=${loginInfo?.userName}]")
        }
        return loginInfo
    }

    /**
     * 普通用户只能看到被授予的任务组，管理员保持原列表不变。
     */
    fun filterExecutorByPermission(request: HttpServletRequest, executorListTotal: List<Executor>): List<Executor> {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithAttr(request)
        val loginInfo = loginInfoResponse.data ?: return ArrayList()
        if (TaskPilotAuthHelper.hasRole(loginInfo, Consts.ADMIN_ROLE).isSuccess) {
            return executorListTotal
        }

        val extraInfo = loginInfo.extraInfo
        val executorIds: List<String> = if (extraInfo != null && extraInfo["executorIds"] != null) {
            StringTool.split(extraInfo["executorIds"], ",")
        } else {
            ArrayList()
        } ?: emptyList()
        return executorListTotal.filter { executorIds.contains(it.id.toString()) }
    }
}
