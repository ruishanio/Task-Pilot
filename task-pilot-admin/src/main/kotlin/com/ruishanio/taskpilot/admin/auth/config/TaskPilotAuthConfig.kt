package com.ruishanio.taskpilot.admin.auth.config

import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.admin.auth.interceptor.TaskPilotAuthInterceptor
import com.ruishanio.taskpilot.admin.auth.store.impl.DbLoginStore
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.annotation.Resource
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
    @Value("\${task-pilot.auth.token.key}")
    private var tokenKey: String = ""

    @Value("\${task-pilot.auth.token.timeout}")
    private var tokenTimeout: Long = 0

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
        registry.addInterceptor(TaskPilotAuthInterceptor()).addPathPatterns("/**")
    }
}
