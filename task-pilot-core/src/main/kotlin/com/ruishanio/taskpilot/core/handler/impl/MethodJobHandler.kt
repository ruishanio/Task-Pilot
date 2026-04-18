package com.ruishanio.taskpilot.core.handler.impl

import com.ruishanio.taskpilot.core.handler.IJobHandler
import java.lang.reflect.Method

/**
 * 基于反射方法的任务处理器。
 *
 * 继续允许调用无参方法和仅引用类型参数的方法，保持旧版方法型任务的接入方式不变。
 */
class MethodJobHandler(
    private val target: Any,
    private val method: Method,
    private var initMethod: Method?,
    private var destroyMethod: Method?
) : IJobHandler() {
    @Throws(Exception::class)
    override fun execute() {
        val paramTypes = method.parameterTypes
        if (paramTypes.isNotEmpty()) {
            // 保持旧逻辑：为每个引用类型参数传入 null，占位执行。
            method.invoke(target, *arrayOfNulls<Any>(paramTypes.size))
        } else {
            method.invoke(target)
        }
    }

    @Throws(Exception::class)
    override fun init() {
        initMethod?.invoke(target)
    }

    @Throws(Exception::class)
    override fun destroy() {
        destroyMethod?.invoke(target)
    }

    override fun toString(): String = "${super.toString()}[${target.javaClass}#${method.name}]"
}
