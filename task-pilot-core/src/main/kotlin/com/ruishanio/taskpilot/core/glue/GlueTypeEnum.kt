package com.ruishanio.taskpilot.core.glue

/**
 * GLUE 任务类型定义。
 *
 * 直接暴露 Kotlin 属性，避免再维护一组仅服务于旧式 getter 的包装方法。
 */
enum class GlueTypeEnum(
    val desc: String,
    val isScript: Boolean,
    val cmd: String?,
    val suffix: String?
) {
    BEAN("BEAN", false, null, null),
    GLUE_GROOVY("GLUE(Java)", false, null, null),
    GLUE_SHELL("GLUE(Shell)", true, "bash", ".sh"),
    GLUE_PYTHON("GLUE(Python3)", true, "python3", ".py"),
    GLUE_PYTHON2("GLUE(Python2)", true, "python", ".py"),
    GLUE_NODEJS("GLUE(Nodejs)", true, "node", ".js"),
    GLUE_POWERSHELL("GLUE(PowerShell)", true, "powershell", ".ps1"),
    GLUE_PHP("GLUE(PHP)", true, "php", ".php");

    companion object {
        /**
         * 按名称匹配 GLUE 类型，未命中时返回 null，保持历史行为。
         */
        fun match(name: String?): GlueTypeEnum? = entries.firstOrNull { it.name == name }
    }
}
