package com.ruishanio.taskpilot.tool.error

/**
 * 业务异常基类，保持最小构造器集合，兼容现有工具层按消息或根因直接抛出。
 */
class BizException : RuntimeException {
    constructor()

    constructor(message: String?) : super(message)

    constructor(cause: Throwable?) : super(cause)
}
