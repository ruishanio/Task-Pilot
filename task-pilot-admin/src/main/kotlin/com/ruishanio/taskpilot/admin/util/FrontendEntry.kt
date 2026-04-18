package com.ruishanio.taskpilot.admin.util

/**
 * 前端静态入口。
 *
 * 所有历史页面路由统一跳到这一份 SPA 入口，避免后端继续输出模板页面。
 */
object FrontendEntry {
    private const val INDEX_PATH = "forward:/web/index.html"

    /**
     * 直接 forward 到 SPA 入口，让浏览器地址保持真实的 history 路径。
     */
    fun index(): String = INDEX_PATH
}
