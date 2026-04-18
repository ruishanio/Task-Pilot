package com.ruishanio.taskpilot.executor.jobhandler

import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.core.handler.annotation.TaskPilot
import com.ruishanio.taskpilot.tool.json.GsonTool
import io.github.imfangs.dify.client.DifyClientFactory
import io.github.imfangs.dify.client.enums.ResponseMode
import io.github.imfangs.dify.client.model.workflow.WorkflowRunRequest
import jakarta.annotation.Resource
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.stereotype.Component

/**
 * AI 任务开发示例。
 */
@Component
class AITaskPilotJob {
    @field:Resource
    private lateinit var ollamaChatModel: OllamaChatModel

    /**
     * 1、Ollama Chat 任务。
     *
     * 参数示例：
     * <pre>
     * {
     *     "input": "{输入信息，必填信息}",
     *     "prompt": "{模型 prompt，可选信息}"
     * }
     * </pre>
     */
    @TaskPilot("ollamaJobHandler")
    fun ollamaJobHandler() {
        val param = TaskPilotHelper.getJobParam()
        if (param.isNullOrBlank()) {
            TaskPilotHelper.log("param is empty.")
            TaskPilotHelper.handleFail()
            return
        }

        val ollamaParam: OllamaParam? = try {
            GsonTool.fromJson(param, OllamaParam::class.java)
        } catch (e: Exception) {
            TaskPilotHelper.log(RuntimeException("OllamaParam parse error", e))
            TaskPilotHelper.handleFail()
            return
        }

        if (ollamaParam == null) {
            TaskPilotHelper.log("OllamaParam parse fail.")
            TaskPilotHelper.handleFail()
            return
        }

        /**
         * 先把可空配置收敛成局部非空值，避免平台类型和可变属性破坏 Kotlin 空安全推导。
         */
        val prompt = ollamaParam.prompt?.takeUnless(String::isBlank) ?: "你是一个研发工程师，擅长解决技术类问题。"
        val input = ollamaParam.input?.takeUnless(String::isBlank)
        if (input == null) {
            TaskPilotHelper.log("input is empty.")
            TaskPilotHelper.handleFail()
            return
        }
        val model = ollamaParam.model?.takeUnless(String::isBlank) ?: "qwen3:0.6b"

        TaskPilotHelper.log("<br><br><b>【Input】: $input</b><br><br>")

        val responseContent = buildChatClient(model)
            .prompt(prompt)
            .user(input)
            .call()
            .content()
            .orEmpty()

        TaskPilotHelper.log("<br><br><b>【Output】: $responseContent</b><br><br>")
    }

    /**
     * 2、Dify Workflow 任务。
     *
     * 参数示例：
     * <pre>
     * {
     *     "inputs": {
     *         "input": "{用户输入信息}"
     *     },
     *     "user": "{用户标识，选填}"
     * }
     * </pre>
     */
    @TaskPilot("difyWorkflowJobHandler")
    fun difyWorkflowJobHandler() {
        val param = TaskPilotHelper.getJobParam()
        if (param.isNullOrBlank()) {
            TaskPilotHelper.log("param is empty.")
            TaskPilotHelper.handleFail()
            return
        }

        val difyParam: DifyParam? = try {
            GsonTool.fromJson(param, DifyParam::class.java)
        } catch (e: Exception) {
            TaskPilotHelper.log(RuntimeException("DifyParam parse error", e))
            TaskPilotHelper.handleFail()
            return
        }

        if (difyParam == null) {
            TaskPilotHelper.log("DifyParam parse fail.")
            TaskPilotHelper.handleFail()
            return
        }

        val inputs = difyParam.inputs ?: LinkedHashMap()
        val user = difyParam.user ?: "task-pilot"
        val baseUrl = difyParam.baseUrl
        val apiKey = difyParam.apiKey
        if (baseUrl == null || apiKey == null) {
            TaskPilotHelper.log("baseUrl or apiKey invalid.")
            TaskPilotHelper.handleFail()
            return
        }

        TaskPilotHelper.log("<br><br><b>【inputs】: $inputs</b><br><br>")

        val request = WorkflowRunRequest.builder()
            .inputs(inputs)
            .responseMode(ResponseMode.BLOCKING)
            .user(user)
            .build()

        val workflowClient = DifyClientFactory.createWorkflowClient(baseUrl, apiKey)
        val workflowResponse = workflowClient.runWorkflow(request)
        TaskPilotHelper.log("<br><br><b>【Output】: ${workflowResponse.data.outputs}</b><br><br>")
    }

    /**
     * 统一封装聊天客户端构建，确保任务执行与控制器示例使用同一套默认能力。
     */
    private fun buildChatClient(model: String): ChatClient =
        ChatClient.builder(ollamaChatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
            .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
            .defaultOptions(OllamaChatOptions.builder().model(model).build())
            .build()

    private class OllamaParam {
        var input: String? = null
        var prompt: String? = null
        var model: String? = null
    }

    private class DifyParam {
        /**
         * Dify input，允许传入 Dify App 定义的各变量值。
         */
        var inputs: MutableMap<String, Any>? = null

        /**
         * Dify user。
         */
        var user: String? = null

        /**
         * Dify baseUrl。
         */
        var baseUrl: String? = null

        /**
         * Dify apiKey。
         */
        var apiKey: String? = null
    }
}
