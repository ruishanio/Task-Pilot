package com.ruishanio.taskpilot.admin.auth.config

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.interceptor.TaskPilotAuthInterceptor
import com.ruishanio.taskpilot.admin.auth.store.impl.DbLoginStore
import com.ruishanio.taskpilot.tool.core.StringTool
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 管理端本地认证配置。
 *
 * 认证参数统一收口到 `ruishan.task-pilot.auth.*`，并继续为前后端分离开发场景保留可配置的跨域来源。
 */
@Configuration
class TaskPilotAuthConfig : WebMvcConfigurer {
    @Value("\${ruishan.task-pilot.auth.token.key}")
    private var tokenKey: String = ""

    @Value("\${ruishan.task-pilot.auth.token.timeout}")
    private var tokenTimeout: Long = 0

    @Value("\${ruishan.task-pilot.auth.excluded-paths:}")
    private var excludedPaths: String = ""

    @Value("\${ruishan.task-pilot.auth.login-path}")
    private var loginPath: String = ""

    @Value("\${ruishan.task-pilot.frontend.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}")
    private var frontendAllowedOriginPatterns: String = ""

    @Resource
    private lateinit var loginStore: DbLoginStore

    /**
     * 启动时初始化本地认证上下文，避免控制器和拦截器各自注入状态。
     */
    @PostConstruct
    fun initAuth() {
        loginStore.start()
        TaskPilotAuthHelper.init(loginStore, tokenKey, tokenTimeout)
    }

    @PreDestroy
    fun destroyAuth() {
        loginStore.stop()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(TaskPilotAuthInterceptor(excludedPaths, loginPath)).addPathPatterns("/**")
    }

    /**
     * 开发态前后端分离需要携带 Cookie，这里显式放开可配置来源。
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        val allowedOriginPatterns = frontendAllowedOriginPatterns.split(",")
            .map { it.trim() }
            .filter { StringTool.isNotBlank(it) }
            .toTypedArray()
        if (allowedOriginPatterns.isEmpty()) {
            return
        }

        registry.addMapping("/**")
            .allowedOriginPatterns(*allowedOriginPatterns)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
