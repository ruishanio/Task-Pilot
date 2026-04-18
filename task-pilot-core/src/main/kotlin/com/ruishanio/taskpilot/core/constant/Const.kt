package com.ruishanio.taskpilot.core.constant

/**
 * 核心常量定义。
 *
 * 使用 Kotlin `object` 归拢跨模块常量，避免散落 magic number。
 */
object Const {
    /**
     * 远程调用 access token 请求头名称。
     */
    const val TASK_PILOT_ACCESS_TOKEN: String = "TASK-PILOT-ACCESS-TOKEN"

    /**
     * 注册心跳周期，单位秒。
     */
    const val BEAT_TIMEOUT: Int = 30

    /**
     * 注册失活超时时间，沿用历史 3 倍心跳周期。
     */
    const val DEAD_TIMEOUT: Int = BEAT_TIMEOUT * 3
}
