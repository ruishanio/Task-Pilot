package com.ruishanio.taskpilot.tool.http

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Cookie 工具。
 * 把编码、删除和查询逻辑集中在一处，保持 Web 层 cookie 处理规则统一。
 */
object CookieTool {
    private const val COOKIE_MAX_AGE = Int.MAX_VALUE
    private const val COOKIE_PATH = "/"

    /**
     * 统一由底层写入入口处理编码、路径和 HttpOnly，避免多个重载分叉出不同默认值。
     */
    private fun set(
        response: HttpServletResponse,
        key: String,
        value: String,
        domain: String?,
        path: String,
        maxAge: Int,
        isHttpOnly: Boolean
    ) {
        val encodedValue =
            try {
                URLEncoder.encode(value, "utf-8")
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException()
            }

        val cookie = Cookie(key, encodedValue)
        if (domain != null) {
            cookie.domain = domain
        }
        cookie.path = path
        cookie.maxAge = maxAge
        cookie.isHttpOnly = isHttpOnly
        response.addCookie(cookie)
    }

    /** 设置 cookie，支持“记住我”场景。 */
    fun set(response: HttpServletResponse, key: String, value: String, ifRemember: Boolean) {
        val age = if (ifRemember) COOKIE_MAX_AGE else -1
        set(response, key, value, null, COOKIE_PATH, age, true)
    }

    /** 按指定过期时间设置 cookie。 */
    fun set(response: HttpServletResponse, key: String, value: String, maxAge: Int) {
        set(response, key, value, null, COOKIE_PATH, maxAge, true)
    }

    /** 删除指定 cookie，保留通过覆盖空值并设置 `maxAge=0` 的历史删除策略。 */
    fun remove(request: HttpServletRequest, response: HttpServletResponse, key: String) {
        val cookie = get(request, key)
        if (cookie != null) {
            set(response, key, "", null, COOKIE_PATH, 0, true)
        }
    }

    /** 查找指定名称的 cookie。 */
    private fun get(request: HttpServletRequest, key: String): Cookie? {
        val arrCookie = request.cookies
        if (arrCookie != null && arrCookie.isNotEmpty()) {
            for (cookie in arrCookie) {
                if (cookie.name == key) {
                    return cookie
                }
            }
        }
        return null
    }

    /** 查询 cookie 值，并继续按 UTF-8 解码。 */
    fun getValue(request: HttpServletRequest, key: String): String? {
        val cookie = get(request, key) ?: return null

        return try {
            URLDecoder.decode(cookie.value, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }
}
