package com.ruishanio.taskpilot.admin.web.error

import com.ruishanio.taskpilot.admin.web.ManageRoute
import org.springframework.boot.web.error.ErrorPage
import org.springframework.boot.web.error.ErrorPageRegistrar
import org.springframework.boot.web.error.ErrorPageRegistry
import org.springframework.stereotype.Component

/**
 * 统一注册错误页入口。
 */
@Component
class WebErrorPageRegistrar : ErrorPageRegistrar {
    override fun registerErrorPages(registry: ErrorPageRegistry) {
        registry.addErrorPages(ErrorPage(ManageRoute.API_MANAGE_ERROR_PAGE))
    }
}
