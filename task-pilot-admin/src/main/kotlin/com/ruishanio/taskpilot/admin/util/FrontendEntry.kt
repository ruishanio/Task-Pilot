package com.ruishanio.taskpilot.admin.util

/**
 * 前端静态入口。
 *
 * 所有历史页面路由统一跳到这一份 SPA 入口，避免后端继续输出模板页面。
 */
object FrontendEntry {
    private const val INDEX_PATH = "redirect:/static/index.html"

    fun route(hashPath: String): String = "$INDEX_PATH#$hashPath"
}
