package com.ruishanio.taskpilot.admin.controller.web

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.util.FrontendEntry
import com.ruishanio.taskpilot.admin.web.ManageRoute
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 管理端页面入口控制器。
 *
 * 所有页面型地址统一收口到 `/web` 前缀下，并改为匿名进入 SPA，由前端自行通过 bootstrap 判断登录态。
 * 这里只兜底不带文件后缀的 history 路由，避免把 `/web/index.html` 或静态资源目录下的真实文件请求也转发回前端入口。
 */
@Controller
class ManageWebController {
    @RequestMapping(ManageRoute.ROOT)
    @TaskPilotAuth(login = false)
    fun index(): String = "redirect:${ManageRoute.WEB_DASHBOARD}"

    @RequestMapping(value = [ManageRoute.WEB_PREFIX, ManageRoute.WEB_ROOT])
    @TaskPilotAuth(login = false)
    fun webIndex(): String = "redirect:${ManageRoute.WEB_DASHBOARD}"

    /**
     * 统一接管 `/web` 前缀下的前端 history 路由，避免每新增一个页面都要在后端补一条映射。
     */
    @RequestMapping(value = [ManageRoute.WEB_FALLBACK, ManageRoute.WEB_NESTED_FALLBACK])
    @TaskPilotAuth(login = false)
    fun frontendRoute(): String = FrontendEntry.index()
}
