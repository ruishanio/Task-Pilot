package com.ruishanio.taskpilot.admin.auth.annotation

/**
 * 管理端本地认证注解。
 *
 * 典型用法：
 * 1、`@TaskPilotAuth`：要求登录；
 * 2、`@TaskPilotAuth(login = false)`：无需登录；
 * 3、`@TaskPilotAuth(permission = "user:add")`：要求具备指定权限；
 * 4、`@TaskPilotAuth(role = "admin")`：要求具备指定角色。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TaskPilotAuth(
    /**
     * 是否要求登录。
     */
    val login: Boolean = true,
    /**
     * 所需权限值。
     */
    val permission: String = "",
    /**
     * 所需角色值。
     */
    val role: String = ""
)
