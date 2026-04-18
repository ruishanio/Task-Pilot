package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotLog
import jakarta.annotation.Resource
import java.util.Date
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖执行日志 Mapper 的分页与状态更新。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskPilotLogMapperTest {
    @field:Resource
    private lateinit var taskPilotLogMapper: TaskPilotLogMapper

    @Test
    fun test() {
        val list = taskPilotLogMapper.pageList(0, 10, 1, 1, null, null, 1)
        val listCount = taskPilotLogMapper.pageListCount(0, 10, 1, 1, null, null, 1)

        val log =
            TaskPilotLog().apply {
                jobGroup = 1
                jobId = 1
            }

        var ret1 = taskPilotLogMapper.save(log)
        var dto = taskPilotLogMapper.load(log.id)

        log.triggerTime = Date()
        log.triggerCode = 1
        log.triggerMsg = "1"
        log.executorAddress = "1"
        log.executorHandler = "1"
        log.executorParam = "1"
        taskPilotLogMapper.updateTriggerInfo(log)
        dto = taskPilotLogMapper.load(log.id)

        log.handleTime = Date()
        log.handleCode = 2
        log.handleMsg = "2"
        ret1 = taskPilotLogMapper.updateHandleInfo(log).toLong()
        dto = taskPilotLogMapper.load(log.id)

        val ret4 = taskPilotLogMapper.findClearLogIds(1, 1, Date(), 100, 100)
        val ret2 = taskPilotLogMapper.delete(log.jobId)

        println("list=$list, listCount=$listCount, ret1=$ret1, dto=$dto, ret4=$ret4, ret2=$ret2")
    }
}
