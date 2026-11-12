package com.ruishanio.taskpilot.admin.constant

/**
 * 管理端常量定义。
 *
 * 管理端局部常量集中放在单一对象中，减少权限相关字符串分散定义。
 */
object Consts {
    const val ADMIN_ROLE: String = "ADMIN"

    // 执行器名称的表单校验与自动同步共用上限，与数据库字段长度保持一致。
    const val EXECUTOR_TITLE_MAX_LENGTH: Int = 64
}
