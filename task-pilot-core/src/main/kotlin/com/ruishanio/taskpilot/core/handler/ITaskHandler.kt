package com.ruishanio.taskpilot.core.handler

/**
 * 任务处理器抽象基类。
 *
 * 继续保留 `execute/init/destroy` 三段生命周期，避免脚本任务与方法任务的接入方式变化。
 */
abstract class ITaskHandler {
    /**
     * 执行任务主体逻辑。
     */
    @Throws(Exception::class)
    abstract fun execute()

    /**
     * 在线程初始化阶段执行扩展逻辑。
     */
    @Throws(Exception::class)
    open fun init() {
        // 默认无初始化逻辑，由具体实现按需覆盖。
    }

    /**
     * 在线程销毁阶段执行清理逻辑。
     */
    @Throws(Exception::class)
    open fun destroy() {
        // 默认无销毁逻辑，由具体实现按需覆盖。
    }
}
