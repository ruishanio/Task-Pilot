package com.ruishanio.taskpilot.core.handler.discovery

import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ApplicationContext
import org.springframework.core.MethodIntrospector
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.Method
import java.util.Collections

/**
 * 扫描 Spring 容器中的 `@TaskPilot` 方法。
 *
 * 设计意图：
 * 1、把扫描逻辑从执行器与 Starter 中抽离，避免两边实现漂移；
 * 2、沿用执行器原有的跳过策略，尽量保持兼容行为。
 */
object TaskPilotMethodScanner {
    /**
     * 扫描全部 TaskPilot 方法定义。
     */
    fun scan(applicationContext: ApplicationContext?, excludedPackage: String?): List<TaskPilotMethodDefinition> {
        if (applicationContext == null) {
            return emptyList()
        }

        val excludedPackageList = parseExcludedPackage(excludedPackage)
        val definitions = ArrayList<TaskPilotMethodDefinition>()

        for (beanName in applicationContext.getBeanNamesForType(Any::class.java, false, false)) {
            if (shouldSkipBeanDefinition(applicationContext, beanName, excludedPackageList)) {
                continue
            }

            val beanClass = applicationContext.getType(beanName, false)
            if (beanClass == null) {
                logger.debug(">>>>>>>>>>> task-pilot 方法扫描跳过 beanClass 为空的 Bean，beanName={}", beanName)
                continue
            }

            val annotatedMethods = findAnnotatedMethods(beanClass, beanName)
            if (annotatedMethods.isEmpty()) {
                continue
            }

            val bean = applicationContext.getBean(beanName)
            for ((method, taskPilot) in annotatedMethods) {
                definitions.add(TaskPilotMethodDefinition(beanName, bean, method, taskPilot))
            }
        }

        return definitions
    }

    /**
     * 解析排除包配置，空白项会被忽略。
     */
    private fun parseExcludedPackage(excludedPackage: String?): List<String> {
        val excludedPackageList = ArrayList<String>()
        if (excludedPackage == null) {
            return excludedPackageList
        }

        for (item in excludedPackage.split(",")) {
            if (item.isNotBlank()) {
                excludedPackageList.add(item.trim())
            }
        }
        return excludedPackageList
    }

    /**
     * 兼容原执行器逻辑，按 BeanDefinition 过滤排除包与懒加载 Bean。
     */
    private fun shouldSkipBeanDefinition(
        applicationContext: ApplicationContext,
        beanName: String,
        excludedPackageList: List<String>
    ): Boolean {
        if (applicationContext !is BeanDefinitionRegistry) {
            return false
        }
        if (!applicationContext.containsBeanDefinition(beanName)) {
            return false
        }

        val beanDefinition: BeanDefinition = applicationContext.getBeanDefinition(beanName)
        val beanClassName = beanDefinition.beanClassName
        if (isExcluded(excludedPackageList, beanClassName)) {
            logger.debug(
                ">>>>>>>>>>> task-pilot 方法扫描跳过排除包 Bean，beanName={}, beanClassName={}",
                beanName,
                beanClassName
            )
            return true
        }
        if (beanDefinition.isLazyInit) {
            logger.debug(">>>>>>>>>>> task-pilot 方法扫描跳过懒加载 Bean，beanName={}", beanName)
            return true
        }

        return false
    }

    /**
     * 查找 Bean 上声明的 TaskPilot 方法。
     */
    private fun findAnnotatedMethods(beanClass: Class<*>, beanName: String): Map<Method, TaskPilot> {
        return try {
            MethodIntrospector.selectMethods(
                beanClass,
                MethodIntrospector.MetadataLookup<TaskPilot> { method ->
                    AnnotatedElementUtils.findMergedAnnotation(method, TaskPilot::class.java)
                }
            )
        } catch (ex: Throwable) {
            logger.error(">>>>>>>>>>> task-pilot 解析方法级任务处理器时发生异常，beanName={}", beanName, ex)
            Collections.emptyMap<Method, TaskPilot>()
        }
    }

    /**
     * 判断当前 Bean 是否在排除包内。
     */
    private fun isExcluded(excludedPackageList: List<String>, beanClassName: String?): Boolean {
        if (excludedPackageList.isEmpty() || beanClassName == null) {
            return false
        }

        for (excludedPackage in excludedPackageList) {
            if (beanClassName.startsWith(excludedPackage)) {
                return true
            }
        }
        return false
    }

    /**
     * TaskPilot 方法定义。
     *
     * 显式保留 record 风格访问器，兼容现有 `definition.taskPilot()` 一类调用。
     */
    class TaskPilotMethodDefinition(
        private val beanName: String,
        private val bean: Any,
        private val method: Method,
        private val taskPilot: TaskPilot
    ) {
        fun beanName(): String = beanName

        fun bean(): Any = bean

        fun method(): Method = method

        fun taskPilot(): TaskPilot = taskPilot
    }

    private val logger = LoggerFactory.getLogger(TaskPilotMethodScanner::class.java)
}
