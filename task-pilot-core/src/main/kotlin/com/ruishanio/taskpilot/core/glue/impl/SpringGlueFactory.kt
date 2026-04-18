package com.ruishanio.taskpilot.core.glue.impl

import com.ruishanio.taskpilot.core.executor.impl.TaskPilotSpringExecutor
import com.ruishanio.taskpilot.core.glue.GlueFactory
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Modifier

/**
 * Spring 场景下的 GLUE 工厂。
 *
 * 仅对声明了注入注解的实例字段做依赖注入，避免脚本对象被容器语义过度侵入。
 */
class SpringGlueFactory : GlueFactory() {
    override fun injectService(instance: Any) {
        val applicationContext = TaskPilotSpringExecutor.getApplicationContext() ?: return

        for (field in instance.javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                continue
            }

            var fieldBean: Any? = null

            val resource = AnnotationUtils.getAnnotation(field, Resource::class.java)
            if (resource != null) {
                fieldBean =
                    runCatching {
                        if (!resource.name.isNullOrEmpty()) {
                            applicationContext.getBean(resource.name)
                        } else {
                            applicationContext.getBean(field.name)
                        }
                    }.getOrNull()
                if (fieldBean == null) {
                    fieldBean = applicationContext.getBean(field.type)
                }
            } else if (AnnotationUtils.getAnnotation(field, Autowired::class.java) != null) {
                val qualifier = AnnotationUtils.getAnnotation(field, Qualifier::class.java)
                fieldBean =
                    if (qualifier?.value?.isNotEmpty() == true) {
                        applicationContext.getBean(qualifier.value)
                    } else {
                        applicationContext.getBean(field.type)
                    }
            }

            if (fieldBean != null) {
                field.isAccessible = true
                try {
                    field.set(instance, fieldBean)
                } catch (e: Exception) {
                    logger.error("向 Glue 实例注入 Spring Bean 时发生异常，field={}", field.name, e)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpringGlueFactory::class.java)
    }
}
