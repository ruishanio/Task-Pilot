package com.ruishanio.taskpilot.tool.http.http.enums

/**
 * 常用 HTTP Header 定义，附带默认 User-Agent 常量，保持旧工具链直接引用不变。
 */
enum class Header(val value: String) {
    CONNECTION("Connection"),
    CONTENT_TYPE("Content-Type"),
    ACCEPT_CHARSET("Accept-Charset"),
    COOKIE("Cookie"),
    AUTHORIZATION("Authorization"),
    SET_COOKIE("Set-Cookie"),
    USER_AGENT("User-Agent");

    override fun toString(): String = "Header{value='$value'}"

    companion object {
        const val DEFAULT_USER_AGENT_WIN: String =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
        const val DEFAULT_USER_AGENT_MAC: String =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    }
}
