package com.ruishanio.taskpilot.spring.boot.starter.autoconfigure

import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilotRegister
import com.ruishanio.taskpilot.core.handler.discovery.TaskPilotMethodScanner
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.openapi.model.SyncRequest
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotatedElementUtils

/**
 * 在应用启动完成后，把 @TaskPilot 声明同步为调度中心中的执行器与任务。
 */
class TaskPilotSyncRunner(
    private val properties: TaskPilotProperties,
    private val applicationContext: ApplicationContext
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val adminBizList: List<AdminBiz> = TaskPilotExecutor.getAdminBizList() ?: emptyList()
        if (adminBizList.isEmpty()) {
            logger.warn(">>>>>>>>>>> task-pilot 启动同步跳过，未找到可用的 adminBiz。")
            return
        }

        val request = buildSyncRequest()
        try {
            val response: Response<String>? = adminBizList[0].sync(request)
            if (response?.isSuccess == true) {
                logger.info(
                    ">>>>>>>>>>> task-pilot 启动同步成功，appName={}, result={}",
                    request.appName,
                    response.data
                )
            } else {
                logger.warn(
                    ">>>>>>>>>>> task-pilot 启动同步失败，appName={}, response={}",
                    request.appName,
                    response
                )
            }
        } catch (e: Throwable) {
            logger.error(">>>>>>>>>>> task-pilot 启动同步异常，appName={}", request.appName, e)
        }
    }

    /**
     * 扫描 @TaskPilot 与 @TaskPilotRegister 注解并组装同步请求。
     */
    private fun buildSyncRequest(): SyncRequest {
        val request = SyncRequest()
        request.appName = properties.executor.appname.orEmpty()
        request.groupTitle = resolveGroupTitle()

        for (definition in TaskPilotMethodScanner.scan(applicationContext, properties.executor.excludedpackage)) {
            val taskPilot = definition.taskPilot()
            // 同步元数据改为独立注解，未声明时不参与同步，避免执行定义与调度元数据耦合。
            val registerMetadataSet = AnnotatedElementUtils.findMergedRepeatableAnnotations(
                definition.method(),
                TaskPilotRegister::class.java
            )
            if (registerMetadataSet.isEmpty()) {
                continue
            }

            val hasMultipleRegisters = registerMetadataSet.size > 1
            for (registerMetadata in registerMetadataSet) {
                val taskName = resolveTaskName(registerMetadata, taskPilot.value, hasMultipleRegisters) ?: continue
                val task = SyncRequest.Task()
                task.taskName = taskName
                task.executorHandler = taskPilot.value
                task.taskDesc = if (StringTool.isNotBlank(registerMetadata.taskDesc)) registerMetadata.taskDesc.trim() else taskPilot.value
                task.author = if (StringTool.isNotBlank(registerMetadata.author)) registerMetadata.author.trim() else resolveDefaultTaskAuthor()
                task.alarmEmail = if (StringTool.isNotBlank(registerMetadata.alarmEmail)) registerMetadata.alarmEmail.trim() else resolveDefaultTaskAlarmEmail()
                task.scheduleType = registerMetadata.scheduleType
                task.scheduleConf = registerMetadata.scheduleConf
                task.misfireStrategy = registerMetadata.misfireStrategy
                task.executorRouteStrategy = registerMetadata.executorRouteStrategy
                task.executorParam = registerMetadata.executorParam
                task.executorBlockStrategy = registerMetadata.executorBlockStrategy
                task.executorTimeout = registerMetadata.executorTimeout
                task.executorFailRetryCount = registerMetadata.executorFailRetryCount
                task.childTaskId = registerMetadata.childTaskId
                task.autoStart = registerMetadata.autoStart
                request.tasks.add(task)
            }
        }
        return request
    }

    /**
     * 单个注册声明允许默认回退到 handler；同一方法声明多份注册时必须显式给出 taskName。
     */
    private fun resolveTaskName(
        registerMetadata: TaskPilotRegister,
        handlerName: String,
        hasMultipleRegisters: Boolean
    ): String? {
        if (StringTool.isNotBlank(registerMetadata.taskName)) {
            return registerMetadata.taskName.trim()
        }
        if (!hasMultipleRegisters) {
            return handlerName
        }

        logger.warn(
            ">>>>>>>>>>> task-pilot 跳过同步，方法声明了多个 @TaskPilotRegister 但缺少 taskName。handler={}",
            handlerName
        )
        return null
    }

    /**
     * 统一处理执行器标题默认值，避免业务侧必须重复配置。
     */
    private fun resolveGroupTitle(): String {
        val groupTitle = properties.sync.groupTitle
        return if (StringTool.isNotBlank(groupTitle)) groupTitle.trim() else properties.executor.appname.orEmpty()
    }

    /**
     * 统一处理负责人默认值，避免注解上处处重复书写。
     */
    private fun resolveDefaultTaskAuthor(): String {
        val defaultTaskAuthor = properties.sync.defaultTaskAuthor
        return if (StringTool.isNotBlank(defaultTaskAuthor)) defaultTaskAuthor.trim() else "TASK-PILOT"
    }

    /**
     * 统一处理报警邮箱默认值，允许通过 Starter 配置补齐注解中的空值。
     */
    private fun resolveDefaultTaskAlarmEmail(): String {
        val defaultTaskAlarmEmail = properties.sync.defaultTaskAlarmEmail
        return if (StringTool.isNotBlank(defaultTaskAlarmEmail)) defaultTaskAlarmEmail.trim() else ""
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotSyncRunner::class.java)
    }
}
