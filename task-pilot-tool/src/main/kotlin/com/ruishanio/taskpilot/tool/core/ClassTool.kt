package com.ruishanio.taskpilot.tool.core

import java.util.HashMap
import java.util.IdentityHashMap

/**
 * 类工具。
 * 继续把原始类型、包装类型和字符串解析规则集中在这里，供反射层统一复用。
 */
object ClassTool {
    private val primitiveWrapper2TypeMap: MutableMap<Class<*>, Class<*>> = IdentityHashMap(9)
    private val primitiveType2WrapperMap: MutableMap<Class<*>, Class<*>> = IdentityHashMap(9)
    private val primitiveString2TypMap: MutableMap<String, Class<*>> = HashMap()

    init {
        primitiveWrapper2TypeMap[Boolean::class.javaObjectType] = Boolean::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Byte::class.javaObjectType] = Byte::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Char::class.javaObjectType] = Char::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Double::class.javaObjectType] = Double::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Float::class.javaObjectType] = Float::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Int::class.javaObjectType] = Int::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Long::class.javaObjectType] = Long::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[Short::class.javaObjectType] = Short::class.javaPrimitiveType!!
        primitiveWrapper2TypeMap[java.lang.Void::class.java] = Void.TYPE

        for ((wrapper, primitive) in primitiveWrapper2TypeMap) {
            primitiveType2WrapperMap[primitive] = wrapper
        }

        primitiveString2TypMap["boolean"] = Boolean::class.javaPrimitiveType!!
        primitiveString2TypMap["byte"] = Byte::class.javaPrimitiveType!!
        primitiveString2TypMap["char"] = Char::class.javaPrimitiveType!!
        primitiveString2TypMap["short"] = Short::class.javaPrimitiveType!!
        primitiveString2TypMap["int"] = Int::class.javaPrimitiveType!!
        primitiveString2TypMap["long"] = Long::class.javaPrimitiveType!!
        primitiveString2TypMap["float"] = Float::class.javaPrimitiveType!!
        primitiveString2TypMap["double"] = Double::class.javaPrimitiveType!!
        primitiveString2TypMap["void"] = Void.TYPE
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> getClass(obj: T?): Class<T>? = if (obj == null) null else obj.javaClass as Class<T>
    fun getClassName(obj: Any?, isSimple: Boolean): String? = if (obj == null) null else getClassName(obj.javaClass, isSimple)
    fun getClassName(clazz: Class<*>?, isSimple: Boolean): String? =
        if (clazz == null) null else if (isSimple) clazz.simpleName else clazz.name
    fun getPackageName(clazz: Class<*>?): String? = if (clazz == null) null else getPackageName(clazz.name)
    fun getPackageName(classFullName: String?): String? {
        if (classFullName == null) {
            return null
        }
        val lastDot = classFullName.lastIndexOf('.')
        return if (lastDot < 0) "" else classFullName.substring(0, lastDot)
    }

    /**
     * 赋值判断继续兼容包装类型和原始类型互认，保持旧版反射复制逻辑不变。
     */
    fun isAssignable(leftHandType: Class<*>?, rightHandType: Class<*>?): Boolean {
        if (rightHandType == null || leftHandType == null) {
            return false
        }
        if (leftHandType.isAssignableFrom(rightHandType)) {
            return true
        }

        return if (leftHandType.isPrimitive) {
            val resolvedPrimitive = primitiveWrapper2TypeMap[rightHandType]
            leftHandType == resolvedPrimitive
        } else {
            val resolvedWrapper = primitiveType2WrapperMap[rightHandType]
            resolvedWrapper != null && leftHandType.isAssignableFrom(resolvedWrapper)
        }
    }
    @Throws(ClassNotFoundException::class)
    fun resolveClass(className: String): Class<*> {
        return try {
            Class.forName(className)
        } catch (ex: ClassNotFoundException) {
            primitiveString2TypMap[className] ?: throw ex
        }
    }
    fun isPrimitive(clazz: Class<*>?): Boolean {
        AssertTool.notNull(clazz, "Class must not be null")
        return clazz!!.isPrimitive
    }
    fun isPrimitiveWrapper(clazz: Class<*>?): Boolean {
        AssertTool.notNull(clazz, "Class must not be null")
        return primitiveWrapper2TypeMap.containsKey(clazz)
    }
    fun isPrimitiveOrWrapper(clazz: Class<*>?): Boolean {
        AssertTool.notNull(clazz, "Class must not be null")
        return isPrimitive(clazz) || isPrimitiveWrapper(clazz)
    }
    fun isPrimitiveOrWrapperOrString(clazz: Class<*>?): Boolean = isPrimitiveOrWrapper(clazz) || String::class.java == clazz
}
