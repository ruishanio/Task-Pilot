package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import jakarta.annotation.Resource
import java.util.Date
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖任务定义 Mapper 的分页与 CRUD 场景。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskPilotInfoMapperTest {
    @field:Resource
    private lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Test
    fun pageList() {
        val list = taskPilotInfoMapper.pageList(0, 20, 0, -1, null, null, null)
        val listCount = taskPilotInfoMapper.pageListCount(0, 20, 0, -1, null, null, null)
        logger.info("list: {}", list)
        logger.info("listCount: {}", listCount)

        val list2 = taskPilotInfoMapper.getJobsByGroup(1)
        logger.info("list2: {}", list2)
    }

    @Test
    fun saveLoad() {
        val info =
            TaskPilotInfo().apply {
                jobGroup = 1
                jobDesc = "desc"
                author = "setAuthor"
                alarmEmail = "setAlarmEmail"
                scheduleType = ScheduleTypeEnum.FIX_RATE
                scheduleConf = "33"
                misfireStrategy = MisfireStrategyEnum.DO_NOTHING
                executorRouteStrategy = ExecutorRouteStrategyEnum.FIRST
                executorHandler = "setExecutorHandler"
                executorParam = "setExecutorParam"
                executorBlockStrategy = ExecutorBlockStrategyEnum.SERIAL_EXECUTION
                glueType = "setGlueType"
                glueSource = "setGlueSource"
                glueRemark = "setGlueRemark"
                childJobId = "1"
                addTime = Date()
                updateTime = Date()
                glueUpdatetime = Date()
            }

        val count = taskPilotInfoMapper.save(info)

        val info2 = taskPilotInfoMapper.loadById(info.id)!!
        info.scheduleType = ScheduleTypeEnum.FIX_RATE
        info.scheduleConf = "44"
        info.misfireStrategy = MisfireStrategyEnum.FIRE_ONCE_NOW
        info2.jobDesc = "desc2"
        info2.author = "setAuthor2"
        info2.alarmEmail = "setAlarmEmail2"
        info2.executorRouteStrategy = ExecutorRouteStrategyEnum.FAILOVER
        info2.executorHandler = "setExecutorHandler2"
        info2.executorParam = "setExecutorParam2"
        info2.executorBlockStrategy = ExecutorBlockStrategyEnum.COVER_EARLY
        info2.glueType = "setGlueType2"
        info2.glueSource = "setGlueSource2"
        info2.glueRemark = "setGlueRemark2"
        info2.glueUpdatetime = Date()
        info2.childJobId = "1"
        info2.updateTime = Date()

        val item2 = taskPilotInfoMapper.update(info2)
        taskPilotInfoMapper.delete(info2.id.toLong())

        val list2 = taskPilotInfoMapper.getJobsByGroup(1)
        val ret3 = taskPilotInfoMapper.findAllCount()

        logger.info("count={}, item2={}, list2={}, ret3={}", count, item2, list2, ret3)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotInfoMapperTest::class.java)
    }
}
