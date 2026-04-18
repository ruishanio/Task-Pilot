package com.ruishanio.taskpilot.admin.scheduler.exception

/**
 * 管理端业务异常。
 *
 * 保留无参和消息构造器，兼容现有 Java 控制器直接抛出的用法。
 */
class TaskPilotException : RuntimeException {
    constructor() : super()

    constructor(message: String?) : super(message)
}
