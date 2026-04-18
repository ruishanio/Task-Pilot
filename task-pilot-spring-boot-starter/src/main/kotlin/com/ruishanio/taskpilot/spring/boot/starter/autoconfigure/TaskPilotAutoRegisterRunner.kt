package com.ruishanio.taskpilot.spring.boot.starter.autoconfigure

import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilotRegister
import com.ruishanio.taskpilot.core.handler.discovery.TaskPilotMethodScanner
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.openapi.model.AutoRegisterRequest
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
class TaskPilotAutoRegisterRunner(
    private val properties: TaskPilotProperties,
    private val applicationContext: ApplicationContext
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val adminBizList: List<AdminBiz> = TaskPilotExecutor.getAdminBizList() ?: emptyList()
        if (adminBizList.isEmpty()) {
            logger.warn(">>>>>>>>>>> task-pilot 自动注册跳过，未找到可用的 adminBiz。")
            return
        }

        val request = buildRequest()
        try {
            val response: Response<String>? = adminBizList[0].autoRegister(request)
            if (response?.isSuccess == true) {
                logger.info(
                    ">>>>>>>>>>> task-pilot 自动注册成功，appname={}, result={}",
                    request.appname,
                    response.data
                )
            } else {
                logger.warn(
                    ">>>>>>>>>>> task-pilot 自动注册失败，appname={}, response={}",
                    request.appname,
                    response
                )
            }
        } catch (e: Throwable) {
            logger.error(">>>>>>>>>>> task-pilot 自动注册异常，appname={}", request.appname, e)
        }
    }

    /**
     * 扫描 @TaskPilot 与 @TaskPilotRegister 注解并组装自动注册请求。
     */
    private fun buildRequest(): AutoRegisterRequest {
        val request = AutoRegisterRequest()
        request.appname = properties.executor.appname.orEmpty()
        request.title = resolveGroupTitle()

        for (definition in TaskPilotMethodScanner.scan(applicationContext, properties.executor.excludedpackage)) {
            val taskPilot = definition.taskPilot()
            // 自动注册元数据改为独立注解，未声明时不参与同步，避免执行定义与调度元数据耦合。
            val autoRegister = AnnotatedElementUtils.findMergedAnnotation(
                definition.method(),
                TaskPilotRegister::class.java
            ) ?: continue

            val task = AutoRegisterRequest.Task()
            task.executorHandler = taskPilot.value
            task.jobDesc = if (StringTool.isNotBlank(autoRegister.jobDesc)) autoRegister.jobDesc.trim() else taskPilot.value
            task.author = if (StringTool.isNotBlank(autoRegister.author)) autoRegister.author.trim() else resolveDefaultTaskAuthor()
            task.alarmEmail = if (StringTool.isNotBlank(autoRegister.alarmEmail)) autoRegister.alarmEmail.trim() else resolveDefaultTaskAlarmEmail()
            task.scheduleType = autoRegister.scheduleType
            task.scheduleConf = autoRegister.scheduleConf
            task.misfireStrategy = autoRegister.misfireStrategy
            task.executorRouteStrategy = autoRegister.executorRouteStrategy
            task.executorParam = autoRegister.executorParam
            task.executorBlockStrategy = autoRegister.executorBlockStrategy
            task.executorTimeout = autoRegister.executorTimeout
            task.executorFailRetryCount = autoRegister.executorFailRetryCount
            task.childJobId = autoRegister.childJobId
            task.autoStart = autoRegister.autoStart
            request.tasks.add(task)
        }
        return request
    }

    /**
     * 统一处理执行器标题默认值，避免业务侧必须重复配置。
     */
    private fun resolveGroupTitle(): String {
        val groupTitle = properties.autoRegister.groupTitle
        return if (StringTool.isNotBlank(groupTitle)) groupTitle.trim() else properties.executor.appname.orEmpty()
    }

    /**
     * 统一处理负责人默认值，避免注解上处处重复书写。
     */
    private fun resolveDefaultTaskAuthor(): String {
        val defaultTaskAuthor = properties.autoRegister.defaultTaskAuthor
        return if (StringTool.isNotBlank(defaultTaskAuthor)) defaultTaskAuthor.trim() else "TASK-PILOT"
    }

    /**
     * 统一处理报警邮箱默认值，允许通过 Starter 配置补齐注解中的空值。
     */
    private fun resolveDefaultTaskAlarmEmail(): String {
        val defaultTaskAlarmEmail = properties.autoRegister.defaultTaskAlarmEmail
        return if (StringTool.isNotBlank(defaultTaskAlarmEmail)) defaultTaskAlarmEmail.trim() else ""
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotAutoRegisterRunner::class.java)
    }
}
