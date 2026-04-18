package com.ruishanio.taskpilot.tool.serializer.impl

import com.ruishanio.taskpilot.tool.serializer.Serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * 基于 JDK 原生对象流的序列化实现。
 * 这里显式保留空值校验与 RuntimeException 包装，避免旧调用方感知异常类型变化。
 */
class JavaSerializer : Serializer() {
    override fun <T> serialize(obj: T): ByteArray {
        if (obj == null) {
            throw RuntimeException("Cannot serialize null object")
        }
        try {
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos ->
                    oos.writeObject(obj)
                    return baos.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to serialize object: ${e.message}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(bytes: ByteArray?): T {
        if (bytes == null) {
            throw RuntimeException("Cannot deserialize null byte array")
        }
        try {
            ByteArrayInputStream(bytes).use { bais ->
                ObjectInputStream(bais).use { ois ->
                    return ois.readObject() as T
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to deserialize object: ${e.message}", e)
        }
    }
}
