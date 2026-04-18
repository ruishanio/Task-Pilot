package com.ruishanio.taskpilot.tool.response

/**
 * 通用响应码定义，继续保留 `code/msg` 访问方式，兼容 Java 侧枚举 getter 调用。
 */
enum class ResponseCode(val code: Int, val msg: String) {
    SUCCESS(200, "Success"),
    FAILURE(500, "Fail"),
    CODE_400(400, "Invalid Argument"),
    CODE_401(401, "Not Authorized"),
    CODE_500(FAILURE.code, FAILURE.msg),
    CODE_501(501, "Server Error"),
    CODE_502(502, "Server Unavailable")
}
