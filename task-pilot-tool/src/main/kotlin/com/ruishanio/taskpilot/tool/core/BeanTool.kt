package com.ruishanio.taskpilot.tool.core

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap

/**
 * Bean 工具。
 * 继续走“字段级反射映射”路线，不引入 getter/setter 约定，保证旧模型类也能直接工作。
 */
object BeanTool {
    /**
     * Bean 字段转 Map 时继续递归展开集合、Map 和复杂对象，输出尽量贴近原始结构。
     */
    fun convertBeanFieldToMap(value: Any?): Any? {
        var finalValue = value
        if (finalValue != null && !ClassTool.isPrimitiveOrWrapperOrString(finalValue.javaClass)) {
            finalValue =
                when (finalValue) {
                    is Collection<*> -> {
                        val result = ArrayList<Any?>()
                        for (item in finalValue) {
                            result.add(convertBeanFieldToMap(item))
                        }
                        result
                    }
                    is Map<*, *> -> {
                        val result = HashMap<Any?, Any?>()
                        for ((mapKey, mapValue) in finalValue) {
                            val convertedKey = convertBeanFieldToMap(mapKey)
                            val convertedValue = convertBeanFieldToMap(mapValue)
                            result[convertedKey] = convertedValue
                        }
                        result
                    }
                    else -> beanToMap(finalValue)
                }
        }
        return finalValue
    }

    /**
     * Map 字段回填 Bean 时，继续只处理基础类型、枚举和嵌套 Map，不做更复杂的泛型集合推断。
     */
    @Suppress("UNCHECKED_CAST")
    fun convertMapFieldToBean(value: Any?, targetClass: Class<*>): Any? {
        if (value == null) {
            return null
        }
        if (targetClass.isAssignableFrom(value.javaClass)) {
            return value
        }

        if (targetClass == Boolean::class.javaPrimitiveType || targetClass == Boolean::class.javaObjectType) {
            return if (value is Boolean) value else value.toString().toBoolean()
        } else if (targetClass == Byte::class.javaPrimitiveType || targetClass == Byte::class.javaObjectType) {
            return if (value is Number) value.toByte() else value.toString().toByte()
        } else if (targetClass == Short::class.javaPrimitiveType || targetClass == Short::class.javaObjectType) {
            return if (value is Number) value.toShort() else value.toString().toShort()
        } else if (targetClass == Int::class.javaPrimitiveType || targetClass == Int::class.javaObjectType) {
            return if (value is Number) value.toInt() else value.toString().toInt()
        } else if (targetClass == Long::class.javaPrimitiveType || targetClass == Long::class.javaObjectType) {
            return if (value is Number) value.toLong() else value.toString().toLong()
        } else if (targetClass == Float::class.javaPrimitiveType || targetClass == Float::class.javaObjectType) {
            return if (value is Number) value.toFloat() else value.toString().toFloat()
        } else if (targetClass == Double::class.javaPrimitiveType || targetClass == Double::class.javaObjectType) {
            return if (value is Number) value.toDouble() else value.toString().toDouble()
        } else if (targetClass == Char::class.javaPrimitiveType || targetClass == Char::class.javaObjectType) {
            return if (value is Char) value else value.toString().let { if (it.isEmpty()) '\u0000' else it[0] }
        } else if (targetClass == String::class.java) {
            return value.toString()
        }

        if (targetClass.isEnum) {
            @Suppress("UNCHECKED_CAST")
            val enumClass = targetClass as Class<out Enum<*>>
            return java.lang.Enum.valueOf(enumClass, value.toString())
        }

        if (value is Map<*, *>) {
            return mapToBean(value as Map<String, Any?>, targetClass)
        }

        return value
    }
    fun beanToMap(bean: Any?, vararg properties: String): Map<String, Any?>? {
        if (bean == null) {
            return null
        }
        val resultMap = HashMap<String, Any?>()
        val fields = ReflectionTool.getFields(bean.javaClass, false)
        val propertySet = HashSet<String>()
        if (properties.isNotEmpty()) {
            propertySet.addAll(Arrays.asList(*properties))
        }

        for (field in fields) {
            if (Modifier.isStatic(field.modifiers)) {
                continue
            }
            if (propertySet.isNotEmpty() && !propertySet.contains(field.name)) {
                continue
            }
            try {
                field.isAccessible = true
                var value = field.get(bean)
                value = convertBeanFieldToMap(value)
                resultMap[field.name] = value
            } catch (e: IllegalAccessException) {
                throw RuntimeException("beanToMap error, failed to get field value: " + field.name, e)
            }
        }
        return resultMap
    }
    fun <T> mapToBean(map: Map<String, Any?>?, targetClass: Class<T>?, vararg properties: String): T? {
        if (map == null || targetClass == null) {
            return null
        }

        return try {
            val instance = targetClass.getDeclaredConstructor().newInstance()
            val fields = ReflectionTool.getFields(targetClass, false)
            val propertySet = HashSet<String>()
            if (properties.isNotEmpty()) {
                propertySet.addAll(Arrays.asList(*properties))
            }

            for (field in fields) {
                if (Modifier.isStatic(field.modifiers) || Modifier.isFinal(field.modifiers)) {
                    continue
                }
                if (propertySet.isNotEmpty() && !propertySet.contains(field.name)) {
                    continue
                }

                val fieldName = field.name
                if (map.containsKey(fieldName)) {
                    try {
                        field.isAccessible = true
                        val value = map[fieldName]
                        val convertedValue = convertMapFieldToBean(value, field.type)
                        field.set(instance, convertedValue)
                    } catch (e: Exception) {
                        throw RuntimeException("mapToBean error, failed to set field: $fieldName", e)
                    }
                }
            }

            instance
        } catch (e: Exception) {
            throw RuntimeException("Failed to create instance of " + targetClass.simpleName, e)
        }
    }
    fun <T> copyProperties(source: Any?, target: T, vararg ignoreProperties: String): T =
        copyProperties(source, target, false, *ignoreProperties)

    /**
     * 属性复制继续采用浅拷贝，只在字段同名且类型可赋值时覆盖目标值。
     */
    fun <T> copyProperties(source: Any?, target: T, ignoreNull: Boolean, vararg ignoreProperties: String): T {
        if (source == null || target == null) {
            return target
        }

        val ignoreSet: Set<String> =
            if (ignoreProperties.isNotEmpty()) {
                HashSet(Arrays.asList(*ignoreProperties))
            } else {
                emptySet()
            }

        val sourceFields = getAllFieldsExcludeStaticWithCache(source.javaClass)
        val targetFields = getAllFieldsExcludeStaticWithCache(target.javaClass)

        for ((fieldName, targetField) in targetFields) {
            if (ignoreSet.contains(fieldName)) {
                continue
            }

            val sourceField = sourceFields[fieldName]
            if (sourceField != null && ClassTool.isAssignable(targetField.type, sourceField.type)) {
                val value = ReflectionTool.getFieldValue(sourceField, source)
                if (ignoreNull && value == null) {
                    continue
                }
                ReflectionTool.setFieldValue(targetField, target, value)
            }
        }

        return target
    }
    fun <T> copyProperties(source: Any?, targetClass: Class<T>?): T? {
        if (source == null || targetClass == null) {
            return null
        }
        val target =
            try {
                targetClass.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                throw RuntimeException("Create target instance failed: " + targetClass.name, e)
            }
        return copyProperties(source, target)
    }
    fun <S, T> copyListProperties(sourceList: List<S>?, targetClass: Class<T>): List<T> {
        if (sourceList == null || sourceList.isEmpty()) {
            return ArrayList()
        }

        val targetList = ArrayList<T>(sourceList.size)
        for (source in sourceList) {
            // 这里保留原实现的失败语义：复制目标创建失败时直接抛出，而不是悄悄丢元素。
            targetList.add(copyProperties(source, targetClass)!!)
        }
        return targetList
    }
    fun isEmpty(obj: Any?): Boolean {
        return when (obj) {
            null -> true
            is CharSequence -> obj.isEmpty()
            is Collection<*> -> obj.isEmpty()
            is Map<*, *> -> obj.isEmpty()
            is Array<*> -> obj.isEmpty()
            else -> false
        }
    }
    fun isNotEmpty(obj: Any?): Boolean = !isEmpty(obj)

    private val FIELD_CACHE: MutableMap<Class<*>, Map<String, Field>> = ConcurrentHashMap()

    /**
     * 字段缓存超过阈值时继续直接清空，保留原实现优先规避类加载器泄漏的取舍。
     */
    private fun getAllFieldsExcludeStaticWithCache(clazz: Class<*>): Map<String, Field> {
        if (FIELD_CACHE.size > 500) {
            FIELD_CACHE.clear()
        }
        return FIELD_CACHE.computeIfAbsent(clazz) {
            val fieldMap = HashMap<String, Field>()
            var currentClass: Class<*>? = clazz
            while (currentClass != null && currentClass != Any::class.java) {
                for (field in currentClass.declaredFields) {
                    if (Modifier.isStatic(field.modifiers)) {
                        continue
                    }
                    fieldMap.putIfAbsent(field.name, field)
                }
                currentClass = currentClass.superclass
            }
            fieldMap
        }
    }
}
