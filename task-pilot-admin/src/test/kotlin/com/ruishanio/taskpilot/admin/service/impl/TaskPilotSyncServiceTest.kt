package com.ruishanio.taskpilot.admin.service.impl

import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.mapper.RegistryMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.core.openapi.model.SyncRequest
import com.ruishanio.taskpilot.tool.response.Response
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.Collections

/**
 * 覆盖同步服务在“更新已有任务”和“跳过无差异任务”两条分支上的行为。
 */
@ExtendWith(MockitoExtension::class)
class TaskPilotSyncServiceTest {
    @Mock
    private lateinit var executorMapper: ExecutorMapper

    @Mock
    private lateinit var taskInfoMapper: TaskInfoMapper

    @Mock
    private lateinit var registryMapper: RegistryMapper

    @Mock
    private lateinit var taskPilotService: TaskPilotService

    private lateinit var taskPilotSyncService: TaskPilotSyncService

    @BeforeEach
    fun setUp() {
        taskPilotSyncService = TaskPilotSyncService()
        ReflectionTestUtils.setField(taskPilotSyncService, "executorMapper", executorMapper)
        ReflectionTestUtils.setField(taskPilotSyncService, "taskInfoMapper", taskInfoMapper)
        ReflectionTestUtils.setField(taskPilotSyncService, "registryMapper", registryMapper)
        ReflectionTestUtils.setField(taskPilotSyncService, "taskPilotService", taskPilotService)
    }

    @Test
    fun shouldUpdateExistingTaskWhenRegisterConfigChanged() {
        val request = buildSyncRequest("任务新描述")
        val group = buildGroup("旧分组标题", "127.0.0.1:9999")
        val existsTask =
            buildExistingTask().apply {
                taskDesc = "任务旧描述"
                author = "old-author"
                alarmEmail = "old@test.com"
                scheduleConf = "0/5 * * * * ?"
                executorParam = "old-param"
            }

        `when`(executorMapper.loadByAppname("demo-app")).thenReturn(group)
        `when`(registryMapper.findAll(anyInt(), anyObject())).thenReturn(Collections.emptyList())
        `when`(taskInfoMapper.loadByExecutorIdAndTaskName(11, "demoHandler")).thenReturn(existsTask)
        `when`(taskPilotService.update(anyObject(), anyObject())).thenReturn(Response.ofSuccess<String>())

        val response = taskPilotSyncService.sync(request)

        assertTrue(response.isSuccess)
        assertTrue(response.data.orEmpty().contains("updatedTaskCount=1"))
        verify(taskPilotService).update(
            argThatObject { jobInfo: TaskInfo ->
                    jobInfo.id == 22 &&
                        jobInfo.taskName == "demoHandler" &&
                    jobInfo.taskDesc == "任务新描述" &&
                    jobInfo.author == "new-author" &&
                    jobInfo.alarmEmail == "new@test.com" &&
                    jobInfo.scheduleConf == "0/10 * * * * ?" &&
                    jobInfo.executorParam == "new-param"
            },
            anyObject()
        )
        verify(taskPilotService, never()).add(anyObject(), anyObject())
        verify(executorMapper).update(
            argThatObject { executor: Executor ->
                executor.title == "DemoGroup" && executor.addressList == null
            }
        )
    }

    @Test
    fun shouldSkipExistingTaskWhenRegisterConfigNotChanged() {
        val request = buildSyncRequest("任务描述")
        val group = buildGroup("DemoGroup", null)
        val existsTask = buildExistingTask()

        `when`(executorMapper.loadByAppname("demo-app")).thenReturn(group)
        `when`(registryMapper.findAll(anyInt(), anyObject())).thenReturn(Collections.emptyList())
        `when`(taskInfoMapper.loadByExecutorIdAndTaskName(11, "demoHandler")).thenReturn(existsTask)

        val response = taskPilotSyncService.sync(request)

        assertTrue(response.isSuccess)
        assertTrue(response.data.orEmpty().contains("updatedTaskCount=0"))
        assertTrue(response.data.orEmpty().contains("skippedTaskCount=1"))
        verify(taskPilotService, never()).update(anyObject(), anyObject())
        verify(taskPilotService, never()).add(anyObject(), anyObject())
        verify(executorMapper, never()).update(anyObject())
    }

    /**
     * 构造一份最小可用的同步请求，测试只覆盖自己关心的差异字段。
     */
    private fun buildSyncRequest(taskDesc: String): SyncRequest =
        SyncRequest().apply {
            appName = "demo-app"
            groupTitle = "DemoGroup"
            val task =
                SyncRequest.Task().apply {
                    executorHandler = "demoHandler"
                    this.taskDesc = taskDesc
                    author = "new-author"
                    alarmEmail = "new@test.com"
                    scheduleType = ScheduleTypeEnum.CRON
                    scheduleConf = "0/10 * * * * ?"
                    misfireStrategy = MisfireStrategyEnum.DO_NOTHING
                    executorRouteStrategy = ExecutorRouteStrategyEnum.FIRST
                    executorParam = "new-param"
                    executorBlockStrategy = ExecutorBlockStrategyEnum.SERIAL_EXECUTION
                    executorTimeout = 30
                    executorFailRetryCount = 2
                    childTaskId = "1,2"
                }
            tasks.add(task)
        }

    private fun buildGroup(title: String, addressList: String?): Executor =
        Executor().apply {
            id = 11
            appname = "demo-app"
            this.title = title
            addressType = 0
            this.addressList = addressList
        }

    /**
     * 构造与 buildSyncRequest 默认值一致的任务快照，便于只覆盖本次测试关心的差异字段。
     */
    private fun buildExistingTask(): TaskInfo =
        TaskInfo().apply {
            id = 22
            executorId = 11
            taskName = "demoHandler"
            taskDesc = "任务描述"
            author = "new-author"
            alarmEmail = "new@test.com"
            scheduleType = ScheduleTypeEnum.CRON
            scheduleConf = "0/10 * * * * ?"
            misfireStrategy = MisfireStrategyEnum.DO_NOTHING
            executorRouteStrategy = ExecutorRouteStrategyEnum.FIRST
            executorHandler = "demoHandler"
            executorParam = "new-param"
            executorBlockStrategy = ExecutorBlockStrategyEnum.SERIAL_EXECUTION
            executorTimeout = 30
            executorFailRetryCount = 2
            childTaskId = "1,2"
        }

    /**
     * 兼容 Kotlin 非空参数与 Mockito `any()` 的配合写法，避免 matcher 返回 null 直接触发 NPE。
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T {
        any<T>()
        return null as T
    }

    /**
     * 兼容 Kotlin 非空参数与 Mockito `argThat()` 的配合写法，避免 matcher 返回 null 直接触发 NPE。
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> argThatObject(predicate: (T) -> Boolean): T {
        argThat<T> { candidate -> predicate(candidate) }
        return null as T
    }
}
