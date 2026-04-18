package com.ruishanio.taskpilot.tool.response

import java.io.Serializable

/**
 * 分页结果模型，字段名保持历史拼写，避免 MyBatis 与前端分页参数映射漂移。
 */
class PageModel<T> : Serializable {
    var offset: Int = 0
    var pagesize: Int = 0
    var data: List<T>? = null
    var total: Int = 0

    override fun toString(): String =
        "PageModel{" +
            "offset=$offset, pagesize=$pagesize, data=$data, total=$total" +
            "}"

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
