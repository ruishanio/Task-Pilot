package com.ruishanio.taskpilot.tool.core

import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * 反射工具。
 * 继续提供“查找 + 可访问化 + 调用/读写”这一套薄封装，避免上层代码重复处理反射异常。
 */
object ReflectionTool {
    fun getMethod(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method? =
        getMethod(clazz, methodName, paramTypes, true, true)
    fun getDeclaredMethod(clazz: Class<*>, methodName: String, vararg paramTypes: Class<*>): Method? =
        getMethod(clazz, methodName, paramTypes, false, true)

    /**
     * 继续只包装 `getMethod/getDeclaredMethod` 的差异，不引入自定义匹配策略，保持查找结果可预测。
     */
    fun getMethod(
        clazz: Class<*>,
        methodName: String,
        paramTypes: Array<out Class<*>>?,
        getMethodOrDeclared: Boolean,
        ignoreIfNotFound: Boolean
    ): Method? {
        AssertTool.notNull(clazz, "Class must not be null")
        AssertTool.notNull(methodName, "Method name must not be null")

        return try {
            if (getMethodOrDeclared) {
                clazz.getMethod(methodName, *(paramTypes ?: emptyArray()))
            } else {
                clazz.getDeclaredMethod(methodName, *(paramTypes ?: emptyArray()))
            }
        } catch (ex: NoSuchMethodException) {
            if (ignoreIfNotFound) {
                null
            } else {
                throw IllegalStateException("Expected method not found: $ex")
            }
        }
    }
    fun getMethods(clazz: Class<*>, getMethodOrDeclared: Boolean): Array<Method> =
        if (getMethodOrDeclared) clazz.methods else clazz.declaredMethods
    fun makeAccessible(method: Method) {
        if (!Modifier.isPublic(method.modifiers) || !Modifier.isPublic(method.declaringClass.modifiers)) {
            method.trySetAccessible()
        }
    }
    fun invokeMethod(method: Method, target: Any?, vararg args: Any?): Any? {
        return try {
            method.invoke(target, *args)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to invoke method [$method]", ex)
        }
    }
    fun getField(clazz: Class<*>, fieldName: String): Field? = getField(clazz, fieldName, true, true)
    fun getDeclaredField(clazz: Class<*>, fieldName: String): Field? = getField(clazz, fieldName, false, true)
    fun getField(
        clazz: Class<*>,
        fieldName: String,
        getFieldOrDeclared: Boolean,
        ignoreIfNotFound: Boolean
    ): Field? {
        AssertTool.notNull(clazz, "Class must not be null")
        AssertTool.notNull(fieldName, "Field name must not be null")

        return try {
            if (getFieldOrDeclared) {
                clazz.getField(fieldName)
            } else {
                clazz.getDeclaredField(fieldName)
            }
        } catch (e: NoSuchFieldException) {
            if (ignoreIfNotFound) {
                null
            } else {
                throw IllegalStateException("Expected field not found: $e")
            }
        }
    }
    fun getFields(clazz: Class<*>, getFieldOrDeclared: Boolean): Array<Field> {
        AssertTool.notNull(clazz, "Class must not be null")
        return if (getFieldOrDeclared) clazz.fields else clazz.declaredFields
    }
    fun isPublicStaticFinal(field: Field): Boolean {
        val modifiers = field.modifiers
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
    }
    fun makeAccessible(field: Field) {
        field.trySetAccessible()
    }
    fun getFieldValue(field: Field, target: Any?): Any? {
        makeAccessible(field)
        return try {
            field.get(target)
        } catch (ex: IllegalAccessException) {
            throw RuntimeException("Failed to get value from field [$field]", ex)
        }
    }
    fun setFieldValue(field: Field, target: Any?, value: Any?) {
        makeAccessible(field)
        try {
            field.set(target, value)
        } catch (ex: IllegalAccessException) {
            throw RuntimeException("Failed to set value to field [$field]", ex)
        }
    }

    /**
     * 字段遍历继续从当前类一路向上追到 `Object`，保持旧版 Bean 处理链覆盖父类字段的行为。
     */
    fun doWithFields(clazz: Class<*>, fieldCallback: FieldCallback) {
        var targetClass: Class<*>? = clazz
        do {
            val fields = getFields(targetClass!!, false)
            for (field in fields) {
                try {
                    fieldCallback.doWith(field)
                } catch (ex: IllegalAccessException) {
                    throw IllegalStateException("Not allowed to access field '" + field.name + "': " + ex)
                }
            }
            targetClass = targetClass.superclass
        } while (targetClass != null && targetClass != Any::class.java)
    }

    interface FieldCallback {
        @Throws(IllegalArgumentException::class, IllegalAccessException::class)
        fun doWith(field: Field)
    }
    fun <T> newProxy(interfaceType: Class<T>, handler: InvocationHandler): T {
        AssertTool.notNull(interfaceType, "Interface type must not be null")
        AssertTool.notNull(handler, "InvocationHandler must not be null")
        AssertTool.isTrue(interfaceType.isInterface, "$interfaceType" + "is not an interface")

        val proxy =
            Proxy.newProxyInstance(
                interfaceType.classLoader,
                arrayOf<Class<*>>(interfaceType),
                handler
            )
        return interfaceType.cast(proxy)
    }
}
