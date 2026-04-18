package com.ruishanio.taskpilot.tool.serializer

import com.ruishanio.taskpilot.tool.serializer.impl.JavaSerializer

/**
 * 序列化策略枚举，目前仍只开放 Java 原生实现，保留后续扩展入口。
 */
enum class SerializerEnum(val serializer: Serializer) {
    JAVA(JavaSerializer());

    companion object {
        /**
         * 名称未命中时回退到默认值，避免上层配置解析因空值直接失败。
         */
        fun match(name: String?, defaultSerializer: SerializerEnum): SerializerEnum =
            entries.firstOrNull { it.name == name } ?: defaultSerializer
    }
}
