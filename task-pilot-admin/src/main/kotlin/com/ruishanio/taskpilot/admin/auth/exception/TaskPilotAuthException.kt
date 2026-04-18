package com.ruishanio.taskpilot.admin.auth.exception

/**
 * 管理端本地认证异常。
 *
 * 保留可写错误码，继续复用统一 Web 异常解析器按业务码输出响应。
 */
class TaskPilotAuthException : RuntimeException {
    var errorCode: Int = 500

    constructor(msg: String) : super(msg)

    constructor(errorCode: Int, msg: String) : super(msg) {
        this.errorCode = errorCode
    }

    constructor(msg: String, cause: Throwable) : super(msg, cause)

    constructor(cause: Throwable) : super(cause)

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
