package com.ruishanio.taskpilot.tool.http.http.enums

import java.nio.charset.Charset

/**
 * 常用 Content-Type 定义，继续保留 `getValue()` 和 `getValue(charset)` 两种读取方式。
 */
enum class ContentType(val value: String) {
    FORM_URLENCODED("application/x-www-form-urlencoded"),
    JSON("application/json"),
    XML("application/xml"),
    TEXT_PLAIN("text/plain"),
    TEXT_XML("text/xml"),
    TEXT_HTML("text/html");

    fun getValue(charset: Charset): String = "$value;charset=${charset.name()}"

    override fun toString(): String = "ContentType{value='$value'}"
}
