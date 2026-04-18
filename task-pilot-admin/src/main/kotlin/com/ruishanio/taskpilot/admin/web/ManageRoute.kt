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
    const val WEB_LOGIN = "$WEB_PREFIX/login"
    const val WEB_DASHBOARD = "$WEB_PREFIX/dashboard"
    const val WEB_JOBGROUP = "$WEB_PREFIX/jobgroup"
    const val WEB_JOBINFO = "$WEB_PREFIX/jobinfo"
    const val WEB_JOBLOG = "$WEB_PREFIX/joblog"
    const val WEB_JOBLOG_DETAIL = "$WEB_PREFIX/joblog/detail"
    const val WEB_JOBCODE = "$WEB_PREFIX/jobcode"
    const val WEB_USER = "$WEB_PREFIX/user"

    const val API_MANAGE_PREFIX = "/api/manage"
    const val API_MANAGE_AUTH = "$API_MANAGE_PREFIX/auth"
    const val API_MANAGE_FRONTEND = "$API_MANAGE_PREFIX/frontend"
    const val API_MANAGE_JOBGROUP = "$API_MANAGE_PREFIX/jobgroup"
    const val API_MANAGE_JOBINFO = "$API_MANAGE_PREFIX/jobinfo"
    const val API_MANAGE_JOBLOG = "$API_MANAGE_PREFIX/joblog"
    const val API_MANAGE_JOBCODE = "$API_MANAGE_PREFIX/jobcode"
    const val API_MANAGE_USER = "$API_MANAGE_PREFIX/user"
    const val API_MANAGE_CHART_INFO = "$API_MANAGE_PREFIX/chartInfo"
    const val API_MANAGE_ERROR_PAGE = "$API_MANAGE_PREFIX/errorpage"
}
