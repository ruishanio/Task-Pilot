package com.ruishanio.taskpilot.tool.id

/**
 * 轻量雪花 ID 生成器，仅保留 workerId 和序列号两段配置。
 * 时钟回拨仍按旧逻辑直接失败，避免静默生成重复 ID。
 */
class SnowflakeIdTool(private val workerId: Long) {
    private var lastTimestamp: Long = -1L
    private var sequence: Long = 0L

    init {
        if (workerId > getMaxWorkerId() || workerId < 0) {
            throw IllegalArgumentException("workerId is illegal")
        }
    }

    @Synchronized
    fun nextId(): Long {
        var timestamp = System.currentTimeMillis()
        if (timestamp < lastTimestamp) {
            throw RuntimeException("时钟回拨")
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0
        }

        lastTimestamp = timestamp
        return (timestamp shl (WORKER_BITS + SEQUENCE_BITS).toInt()) or
            (workerId shl SEQUENCE_BITS.toInt()) or
            sequence
    }

    /**
     * 序列号在同一毫秒内用尽时，阻塞等待到下一个毫秒，保持单机内的有序唯一性。
     */
    private fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }

    private fun getMaxWorkerId(): Long = (-1L shl WORKER_BITS.toInt()).inv()

    companion object {
        private const val WORKER_BITS = 10L
        private const val SEQUENCE_BITS = 12L
        private const val MAX_SEQUENCE = (-1L shl SEQUENCE_BITS.toInt()).inv()
    }
}
