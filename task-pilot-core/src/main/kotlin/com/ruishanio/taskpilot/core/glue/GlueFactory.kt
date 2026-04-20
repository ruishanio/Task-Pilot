package com.ruishanio.taskpilot.core.glue

import com.ruishanio.taskpilot.core.glue.impl.SpringGlueFactory
import com.ruishanio.taskpilot.core.handler.ITaskHandler
import groovy.lang.GroovyClassLoader
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * GLUE 工厂。
 *
 * 负责把脚本源码编译为任务处理器实例，并为不同运行环境切换注入策略。
 */
open class GlueFactory {
    /**
     * Groovy 类加载器与源码缓存均保持实例级，避免不同运行模式共享污染。
     */
    private val groovyClassLoader = GroovyClassLoader()
    private val classCache: ConcurrentMap<String, Class<*>> = ConcurrentHashMap()

    /**
     * 基于源码创建新的任务处理器实例。
     */
    @Throws(Exception::class)
    open fun loadNewInstance(codeSource: String?): ITaskHandler {
        if (!codeSource.isNullOrBlank()) {
            val clazz = getCodeSourceClass(codeSource)
            if (clazz != null) {
                val instance = clazz.getDeclaredConstructor().newInstance()
                if (instance is ITaskHandler) {
                    injectService(instance)
                    return instance
                }
                throw IllegalArgumentException(
                    ">>>>>>>>>>> task-pilot-glue, loadNewInstance error, " +
                        "cannot convert from instance[${instance.javaClass}] to ITaskHandler"
                )
            }
        }
        throw IllegalArgumentException(">>>>>>>>>>> task-pilot-glue, loadNewInstance error, instance is null")
    }

    /**
     * 对源码做缓存编译，优先复用同一份脚本对应的 Class。
     */
    protected open fun getCodeSourceClass(codeSource: String): Class<*>? {
        return try {
            val md5 = MessageDigest.getInstance("MD5").digest(codeSource.toByteArray())
            val md5Str = BigInteger(1, md5).toString(16)
            classCache[md5Str] ?: groovyClassLoader.parseClass(codeSource).also {
                classCache.putIfAbsent(md5Str, it)
            }
        } catch (_: Exception) {
            groovyClassLoader.parseClass(codeSource)
        }
    }

    /**
     * 子类按运行环境覆盖依赖注入逻辑。
     */
    open fun injectService(instance: Any) {
        // 默认无注入逻辑，供无 Spring 场景直接执行。
    }

    companion object {
        private var glueFactory: GlueFactory = GlueFactory()

        /**
         * 获取当前生效的 GLUE 工厂实例。
         */
        fun getInstance(): GlueFactory = glueFactory

        /**
         * 按运行模式切换工厂实现。
         *
         * `0` 表示无框架模式，`1` 表示 Spring 模式。
         */
        fun refreshInstance(type: Int) {
            glueFactory =
                when (type) {
                    1 -> SpringGlueFactory()
                    else -> GlueFactory()
                }
        }
    }
}
