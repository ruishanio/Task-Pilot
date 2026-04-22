package com.ruishanio.taskpilot.admin.web

import com.ruishanio.taskpilot.admin.util.FrontendEntry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 管理端 SPA history 路由转发过滤器。
 *
 * Spring 7 的 PathPattern 不再支持 `/**/{var}` 这类中间带双星的控制器映射，这里改用过滤器兜底 `/web`
 * 前缀下所有不带文件后缀的 GET 请求，既保留前端路由能力，又不会吞掉真实静态资源。
 */
@Component
class ManageSpaForwardFilter : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        if (!"GET".equals(request.method, ignoreCase = true)) {
            return true
        }

        val requestPath = resolveRequestPath(request)
        if (requestPath == ManageRoute.WEB_PREFIX || requestPath == ManageRoute.WEB_ROOT) {
            return true
        }
        if (!requestPath.startsWith("${ManageRoute.WEB_PREFIX}/")) {
            return true
        }

        return requestPath.substringAfter("${ManageRoute.WEB_PREFIX}/").contains(".")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        request.getRequestDispatcher(FrontendEntry.index().removePrefix("forward:")).forward(request, response)
    }

    /**
     * 统一去掉 contextPath，避免后续路径判断在测试环境与真实部署环境下出现偏差。
     */
    private fun resolveRequestPath(request: HttpServletRequest): String =
        request.requestURI.removePrefix(request.contextPath).ifEmpty { "/" }
}
