package com.ruishanio.taskpilot.tool.cache

/**
 * 缓存类型枚举，继续保留原有命名，避免构建器与外部配置值失配。
 */
enum class CacheType {
    NONE,
    FIFO,
    LFU,
    LRU,
    UNLIMITED
}
