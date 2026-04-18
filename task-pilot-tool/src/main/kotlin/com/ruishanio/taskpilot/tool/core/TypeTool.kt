package com.ruishanio.taskpilot.tool.core

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * 类型工具。
 * 这里继续只做“是否可赋值”的轻量判断，不尝试完整实现复杂泛型约束推导。
 */
object TypeTool {
    fun isAssignable(superType: Type?, subType: Type?): Boolean {
        AssertTool.notNull(superType, "Left-hand side type must not be null")
        AssertTool.notNull(subType, "Right-hand side type must not be null")

        if (superType == subType || Any::class.java == superType) {
            return true
        }

        if (superType is Class<*>) {
            val lhsClass = superType
            if (subType is Class<*>) {
                return ClassTool.isAssignable(lhsClass, subType)
            }

            if (subType is ParameterizedType) {
                val rhsRaw = subType.rawType
                if (rhsRaw is Class<*>) {
                    return ClassTool.isAssignable(lhsClass, rhsRaw)
                }
            } else if (lhsClass.isArray && subType is GenericArrayType) {
                val rhsComponent = subType.genericComponentType
                return isAssignable(lhsClass.componentType, rhsComponent)
            }
        }

        if (superType is ParameterizedType) {
            if (subType is Class<*>) {
                val lhsRaw = superType.rawType
                if (lhsRaw is Class<*>) {
                    return ClassTool.isAssignable(lhsRaw, subType)
                }
            } else if (subType is ParameterizedType) {
                return isAssignable(superType as Type, subType as Type)
            }
        }

        if (superType is GenericArrayType) {
            val lhsComponent = superType.genericComponentType
            if (subType is Class<*>) {
                if (subType.isArray) {
                    return isAssignable(lhsComponent, subType.componentType)
                }
            } else if (subType is GenericArrayType) {
                val rhsComponent = subType.genericComponentType
                return isAssignable(lhsComponent, rhsComponent)
            }
        }

        if (superType is WildcardType) {
            return isAssignable(superType as Type, subType)
        }

        return false
    }
}
