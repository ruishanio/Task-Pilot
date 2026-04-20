package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskReport
import com.ruishanio.taskpilot.tool.core.DateTool
import jakarta.annotation.Resource
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖日志汇总 Mapper 的写入入口。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskReportMapperTest {
    @field:Resource
    private lateinit var taskReportMapper: TaskReportMapper

    @Test
    fun test() {
        val date = DateTool.parseDate("2025-10-01")
        val taskReport =
            TaskReport().apply {
                triggerDay = date
                runningCount = 444
                sucCount = 555
                failCount = 666
            }

        val ret = taskReportMapper.saveOrUpdate(taskReport)
        logger.info("ret:{}", ret)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskReportMapperTest::class.java)
    }
}
