package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskLog
import jakarta.annotation.Resource
import java.util.Date
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖执行日志 Mapper 的分页与状态更新。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskLogMapperTest {
    @field:Resource
    private lateinit var taskLogMapper: TaskLogMapper

    @Test
    fun test() {
        val list = taskLogMapper.pageList(0, 10, 1, 1, null, null, 1)
        val listCount = taskLogMapper.pageListCount(0, 10, 1, 1, null, null, 1)

        val log =
            TaskLog().apply {
                executorId = 1
                taskId = 1
            }

        var ret1 = taskLogMapper.save(log)
        var dto = taskLogMapper.load(log.id)

        log.triggerTime = Date()
        log.triggerCode = 1
        log.triggerMsg = "1"
        log.executorAddress = "1"
        log.executorHandler = "1"
        log.executorParam = "1"
        taskLogMapper.updateTriggerInfo(log)
        dto = taskLogMapper.load(log.id)

        log.handleTime = Date()
        log.handleCode = 2
        log.handleMsg = "2"
        ret1 = taskLogMapper.updateHandleInfo(log).toLong()
        dto = taskLogMapper.load(log.id)

        val ret4 = taskLogMapper.findClearLogIds(1, 1, Date(), 100, 100)
        val ret2 = taskLogMapper.delete(log.taskId)

        println("list=$list, listCount=$listCount, ret1=$ret1, dto=$dto, ret4=$ret4, ret2=$ret2")
    }
}
