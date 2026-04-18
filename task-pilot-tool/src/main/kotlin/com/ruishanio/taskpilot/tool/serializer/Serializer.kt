package com.ruishanio.taskpilot.tool.serializer

/**
 * 序列化抽象，继续约束为“对象 <-> 字节数组”双向转换，便于不同实现统一挂载。
 */
abstract class Serializer {
    abstract fun <T> serialize(obj: T): ByteArray

    abstract fun <T> deserialize(bytes: ByteArray?): T
}
