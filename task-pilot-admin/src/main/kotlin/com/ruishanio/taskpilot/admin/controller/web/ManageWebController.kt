package com.ruishanio.taskpilot.admin.controller.web

import com.ruishanio.taskpilot.admin.util.FrontendEntry
import com.ruishanio.taskpilot.admin.web.ManageRoute
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * 管理端页面入口控制器。
 *
 * `/` 与 `/web` 的入口跳转仍由控制器承接，其余 `/web` 前缀下的 history 路由统一交给过滤器转发到 SPA。
 */
@Controller
class ManageWebController {
    @RequestMapping(ManageRoute.ROOT)
    fun index(): String = "redirect:${ManageRoute.WEB_PREFIX}"

    @RequestMapping(ManageRoute.WEB_ROOT)
    fun webIndex(): String = "redirect:${ManageRoute.WEB_PREFIX}"

    /**
     * `/web` 作为前端唯一入口，首次进入时直接交给 SPA，后续路由切换继续走前端 history。
     */
    @RequestMapping(ManageRoute.WEB_PREFIX)
    fun frontendIndex(): String = FrontendEntry.index()
}
