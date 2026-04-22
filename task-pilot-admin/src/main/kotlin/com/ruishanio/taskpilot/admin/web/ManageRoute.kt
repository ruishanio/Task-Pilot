package com.ruishanio.taskpilot.admin.web

/**
 * 管理端路由常量。
 *
 * 统一收口页面入口、管理 API 与执行器协议前缀，避免前后端各自硬编码导致路由规则漂移。
 */
object ManageRoute {
    const val ROOT = "/"

    const val WEB_PREFIX = "/web"
    const val WEB_ROOT = "$WEB_PREFIX/"
    const val WEB_DASHBOARD = "$WEB_PREFIX/dashboard"
    const val WEB_FALLBACK = "$WEB_PREFIX/{path:[^.]*}"
    const val WEB_NESTED_FALLBACK = "$WEB_PREFIX/**/{path:[^.]*}"

    const val API_MANAGE_PREFIX = "/api/manage"
    const val API_MANAGE_AUTH = "$API_MANAGE_PREFIX/auth"
    const val API_MANAGE_SYSTEM = "$API_MANAGE_PREFIX/system"
    const val API_MANAGE_STAT = "$API_MANAGE_PREFIX/stat"
    const val API_MANAGE_EXECUTOR = "$API_MANAGE_PREFIX/executor"
    const val API_MANAGE_TASK_INFO = "$API_MANAGE_PREFIX/task_info"
    const val API_MANAGE_TASK_LOG = "$API_MANAGE_PREFIX/task_log"
    const val API_MANAGE_TASK_CODE = "$API_MANAGE_PREFIX/task_code"
    const val API_MANAGE_USER = "$API_MANAGE_PREFIX/user"
    const val API_MANAGE_CHART_INFO = "$API_MANAGE_PREFIX/chart_info"
    const val API_MANAGE_ERROR_PAGE = "$API_MANAGE_PREFIX/errorpage"
}
