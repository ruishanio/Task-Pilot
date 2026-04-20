package com.ruishanio.taskpilot.spring.boot.starter.autoconfigure

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilotRegister
import com.ruishanio.taskpilot.core.openapi.model.AutoRegisterRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.util.ReflectionTestUtils

/**
 * 覆盖 Starter 从注解扫描到自动注册请求组装的链路，确保枚举配置会按协议值透传。
 */
class TaskPilotAutoRegisterRunnerTest {
    @Test
    fun shouldBuildAutoRegisterRequestWithEnumStrategiesAndDefaults() {
        AnnotationConfigApplicationContext(TestConfiguration::class.java).use { applicationContext ->
            val properties = TaskPilotProperties().apply {
                executor.appname = "demo-executor"
                autoRegister.groupTitle = "示例执行器"
                autoRegister.defaultTaskAuthor = "默认负责人"
                autoRegister.defaultTaskAlarmEmail = "notice@test.com"
            }
            val runner = TaskPilotAutoRegisterRunner(properties, applicationContext)

            val request =
                (ReflectionTestUtils.invokeMethod(runner, "buildRequest") as AutoRegisterRequest?)
                    ?: error("buildRequest should not return null")

            assertEquals("demo-executor", request.appname)
            assertEquals("示例执行器", request.title)
            assertEquals(2, request.tasks.size)

            val taskMap = request.tasks.associateBy { it.executorHandler }
            val explicitTask = taskMap["explicitHandler"]
            assertNotNull(explicitTask)
            assertEquals("显式任务", explicitTask!!.jobDesc)
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
            assertEquals("3,4", explicitTask.childJobId)

            val defaultTask = taskMap["defaultHandler"]
            assertNotNull(defaultTask)
            assertEquals("defaultHandler", defaultTask!!.jobDesc)
            assertEquals("默认负责人", defaultTask.author)
            assertEquals("notice@test.com", defaultTask.alarmEmail)
            assertEquals(ScheduleTypeEnum.CRON, defaultTask.scheduleType)
            assertEquals("0/10 * * * * ?", defaultTask.scheduleConf)
            assertEquals(MisfireStrategyEnum.DO_NOTHING, defaultTask.misfireStrategy)
            assertEquals(ExecutorRouteStrategyEnum.FIRST, defaultTask.executorRouteStrategy)
            assertEquals(ExecutorBlockStrategyEnum.SERIAL_EXECUTION, defaultTask.executorBlockStrategy)
        }
    }

    /**
     * 用两个示例任务同时覆盖“显式枚举配置”和“默认值回退”两条路径。
     */
    @Configuration
    class TestConfiguration {
        @Bean
        fun sampleJobs(): SampleJobs = SampleJobs()
    }

    /**
     * 通过真实注解声明驱动扫描逻辑，避免测试绕开方法发现与合并注解行为。
     */
    class SampleJobs {
        @TaskPilot("explicitHandler")
        @TaskPilotRegister(
            jobDesc = "显式任务",
            author = "测试负责人",
            alarmEmail = "alarm@test.com",
            scheduleType = ScheduleTypeEnum.FIX_RATE,
            scheduleConf = "15",
            misfireStrategy = MisfireStrategyEnum.FIRE_ONCE_NOW,
            executorRouteStrategy = ExecutorRouteStrategyEnum.SHARDING_BROADCAST,
            executorParam = "payload",
            executorBlockStrategy = ExecutorBlockStrategyEnum.COVER_EARLY,
            executorTimeout = 12,
            executorFailRetryCount = 2,
            childJobId = "3,4"
        )
        fun explicitJob() {
        }

        @TaskPilot("defaultHandler")
        @TaskPilotRegister(scheduleConf = "0/10 * * * * ?")
        fun defaultJob() {
        }
    }
}
