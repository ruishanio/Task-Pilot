package com.ruishanio.taskpilot.admin.auth.config

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.interceptor.TaskPilotAuthInterceptor
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 管理端本地认证配置。
 *
 * 认证参数统一收口到 `task-pilot.auth.*`。
 */
@Configuration
class TaskPilotAuthConfig : WebMvcConfigurer {
    @Value($$"${task-pilot.auth.token.timeout}")
    private var tokenTimeout: Long = 0

    @Value($$"${task-pilot.auth.jwt.secret}")
    private var jwtSecret: String = ""

    /**
     * 启动时初始化 JWT 认证上下文，避免控制器和拦截器各自拼装验签逻辑。
     */
    @PostConstruct
    fun initAuth() {
        TaskPilotAuthHelper.init(jwtSecret, tokenTimeout)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(TaskPilotAuthInterceptor()).addPathPatterns("/**")
    }
}
