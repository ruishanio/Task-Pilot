package com.ruishanio.taskpilot.tool.id

import java.util.UUID

/**
 * UUID 工具，保留带横线与无横线两种常用输出格式。
 */
object UUIDTool {
    fun getUUID(): String = UUID.randomUUID().toString()
    fun getSimpleUUID(): String = getUUID().replace("-", "")
}
