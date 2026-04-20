package com.ruishanio.taskpilot.spring.boot.starter.autoconfigure

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilotRegister
import com.ruishanio.taskpilot.core.openapi.model.SyncRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.util.ReflectionTestUtils

/**
 * 覆盖 Starter 从注解扫描到同步请求组装的链路，确保枚举配置会按协议值透传。
 */
class TaskPilotSyncRunnerTest {
    @Test
    fun shouldBuildSyncRequestWithEnumStrategiesAndDefaults() {
        AnnotationConfigApplicationContext(TestConfiguration::class.java).use { applicationContext ->
            val properties = TaskPilotProperties().apply {
                executor.appname = "demo-executor"
                sync.executorTitle = "示例执行器"
                sync.defaultTaskAuthor = "默认负责人"
                sync.defaultTaskAlarmEmail = "notice@test.com"
            }
            val runner = TaskPilotSyncRunner(properties, applicationContext)

            val request =
                (ReflectionTestUtils.invokeMethod(runner, "buildSyncRequest") as SyncRequest?)
                    ?: error("buildSyncRequest should not return null")

            assertEquals("demo-executor", request.appName)
            assertEquals("示例执行器", request.executorTitle)
            assertEquals(4, request.tasks.size)

            val taskMap = request.tasks.associateBy { it.executorHandler }
            val explicitTask = taskMap["explicitHandler"]
            assertNotNull(explicitTask)
            assertEquals("explicit-task", explicitTask!!.taskName)
            assertEquals("显式任务", explicitTask!!.taskDesc)
            assertEquals("测试负责人", explicitTask.author)
            assertEquals("alarm@test.com", explicitTask.alarmEmail)
            assertEquals(ScheduleTypeEnum.FIX_RATE, explicitTask.scheduleType)
            assertEquals("15", explicitTask.scheduleConf)
            assertEquals(MisfireStrategyEnum.FIRE_ONCE_NOW, explicitTask.misfireStrategy)
            assertEquals(ExecutorRouteStrategyEnum.SHARDING_BROADCAST, explicitTask.executorRouteStrategy)
            assertEquals("payload", explicitTask.executorParam)
            assertEquals(ExecutorBlockStrategyEnum.COVER_EARLY, explicitTask.executorBlockStrategy)
            assertEquals(12, explicitTask.executorTimeout)
            assertEquals(2, explicitTask.executorFailRetryCount)
            assertEquals("3,4", explicitTask.childTaskId)

            val defaultTask = taskMap["defaultHandler"]
            assertNotNull(defaultTask)
            assertEquals("defaultHandler", defaultTask!!.taskName)
            assertEquals("defaultHandler", defaultTask!!.taskDesc)
            assertEquals("默认负责人", defaultTask.author)
            assertEquals("notice@test.com", defaultTask.alarmEmail)
            assertEquals(ScheduleTypeEnum.CRON, defaultTask.scheduleType)
            assertEquals("0/10 * * * * ?", defaultTask.scheduleConf)
            assertEquals(MisfireStrategyEnum.DO_NOTHING, defaultTask.misfireStrategy)
            assertEquals(ExecutorRouteStrategyEnum.FIRST, defaultTask.executorRouteStrategy)
            assertEquals(ExecutorBlockStrategyEnum.SERIAL_EXECUTION, defaultTask.executorBlockStrategy)

            val multiTaskMap = request.tasks.filter { it.executorHandler == "multiHandler" }.associateBy { it.taskName }
            assertEquals(2, multiTaskMap.size)
            assertEquals("多注册任务A", multiTaskMap["multi-task-a"]!!.taskDesc)
            assertEquals("5", multiTaskMap["multi-task-a"]!!.scheduleConf)
            assertEquals("多注册任务B", multiTaskMap["multi-task-b"]!!.taskDesc)
            assertEquals("10", multiTaskMap["multi-task-b"]!!.scheduleConf)
        }
    }

    /**
     * 用两个示例任务同时覆盖“显式枚举配置”和“默认值回退”两条路径。
     */
    @Configuration
    class TestConfiguration {
        @Bean
        fun sampleTasks(): SampleTasks = SampleTasks()
    }

    /**
     * 通过真实注解声明驱动扫描逻辑，避免测试绕开方法发现与合并注解行为。
     */
    class SampleTasks {
        @TaskPilot("explicitHandler")
        @TaskPilotRegister(
            taskName = "explicit-task",
            taskDesc = "显式任务",
            author = "测试负责人",
            alarmEmail = "alarm@test.com",
            type = ScheduleTypeEnum.FIX_RATE,
            conf = "15",
            misfire = MisfireStrategyEnum.FIRE_ONCE_NOW,
            route = ExecutorRouteStrategyEnum.SHARDING_BROADCAST,
            param = "payload",
            block = ExecutorBlockStrategyEnum.COVER_EARLY,
            timeout = 12,
            executorFailRetryCount = 2,
            childTaskId = "3,4"
        )
        fun explicitTask() {
        }

        @TaskPilot("defaultHandler")
        @TaskPilotRegister(conf = "0/10 * * * * ?")
        fun defaultTask() {
        }

        @TaskPilot("multiHandler")
        @TaskPilotRegister(taskName = "multi-task-a", taskDesc = "多注册任务A", conf = "5")
        @TaskPilotRegister(taskName = "multi-task-b", taskDesc = "多注册任务B", conf = "10")
        fun multiTask() {
        }
    }
}
