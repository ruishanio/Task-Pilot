package com.ruishanio.taskpilot.core.executor.impl

import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot

/**
 * 无框架场景下的 TaskPilot 执行器。
 */
class TaskPilotSimpleExecutor : TaskPilotExecutor() {
    var taskPilotBeanList: List<Any> = ArrayList()

    override fun start() {
        initTaskHandlerMethodRepository(taskPilotBeanList)
        try {
            super.start()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun destroy() {
        super.destroy()
    }

    /**
     * 从显式传入的 Bean 列表中扫描并注册任务方法。
     */
    private fun initTaskHandlerMethodRepository(taskPilotBeanList: List<Any>?) {
        if (taskPilotBeanList.isNullOrEmpty()) {
            return
        }

        for (bean in taskPilotBeanList) {
            val methods = bean.javaClass.declaredMethods
            if (methods.isEmpty()) {
                continue
            }
            for (executeMethod in methods) {
                val taskPilot = executeMethod.getAnnotation(TaskPilot::class.java)
                registerTaskHandler(taskPilot, bean, executeMethod)
            }
        }
    }
}
