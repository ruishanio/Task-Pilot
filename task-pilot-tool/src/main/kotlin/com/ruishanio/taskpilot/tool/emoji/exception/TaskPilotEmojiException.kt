package com.ruishanio.taskpilot.tool.emoji.exception

/**
 * Emoji 领域异常。
 */
class TaskPilotEmojiException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)
}
