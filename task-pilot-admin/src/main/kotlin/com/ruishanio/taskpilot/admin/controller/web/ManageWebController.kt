package com.ruishanio.taskpilot.admin.controller.web

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.util.FrontendEntry
import com.ruishanio.taskpilot.admin.web.ManageRoute
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 管理端页面入口控制器。
 *
 * 所有页面型地址统一收口到 `/web` 前缀下，真实渲染交给前端 SPA 的 history 路由。
 */
@Controller
class ManageWebController {
    @RequestMapping(ManageRoute.ROOT)
    @TaskPilotAuth
    fun index(): String = "redirect:${ManageRoute.WEB_DASHBOARD}"

    @RequestMapping(value = [ManageRoute.WEB_PREFIX, ManageRoute.WEB_ROOT])
    @TaskPilotAuth
    fun webIndex(): String = "redirect:${ManageRoute.WEB_DASHBOARD}"

    @RequestMapping(ManageRoute.WEB_LOGIN)
    @TaskPilotAuth(login = false)
    fun login(request: HttpServletRequest, response: HttpServletResponse): String {
        val loginInfoResponse = TaskPilotAuthHelper.loginCheckWithCookie(request, response)
        return if (loginInfoResponse.isSuccess) "redirect:${ManageRoute.WEB_DASHBOARD}" else FrontendEntry.index()
    }

    @RequestMapping(ManageRoute.WEB_DASHBOARD)
    @TaskPilotAuth
    fun dashboard(): String = FrontendEntry.index()

    @RequestMapping(ManageRoute.WEB_JOBGROUP)
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun jobGroup(): String = FrontendEntry.index()

    @RequestMapping(ManageRoute.WEB_JOBINFO)
    @TaskPilotAuth
    fun jobInfo(): String = FrontendEntry.index()

    @RequestMapping(ManageRoute.WEB_JOBLOG)
    @TaskPilotAuth
    fun jobLog(): String = FrontendEntry.index()

    /**
     * 日志详情入口交给前端页面自行读取查询参数，后端只负责兜底到 SPA。
     */
    @RequestMapping(ManageRoute.WEB_JOBLOG_DETAIL)
    @TaskPilotAuth
    fun jobLogDetail(): String = FrontendEntry.index()

    @RequestMapping(ManageRoute.WEB_JOBCODE)
    @TaskPilotAuth
    fun jobCode(): String = FrontendEntry.index()

    @RequestMapping(ManageRoute.WEB_USER)
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun user(): String = FrontendEntry.index()
}
