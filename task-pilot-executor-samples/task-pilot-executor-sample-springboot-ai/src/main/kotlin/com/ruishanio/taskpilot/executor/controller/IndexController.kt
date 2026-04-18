package com.ruishanio.taskpilot.executor.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.imfangs.dify.client.DifyClientFactory
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback
import io.github.imfangs.dify.client.enums.ResponseMode
import io.github.imfangs.dify.client.event.ErrorEvent
import io.github.imfangs.dify.client.event.NodeFinishedEvent
import io.github.imfangs.dify.client.event.NodeStartedEvent
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent
import io.github.imfangs.dify.client.event.WorkflowStartedEvent
import io.github.imfangs.dify.client.model.workflow.WorkflowRunRequest
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux

@Controller
@EnableAutoConfiguration
class IndexController {
    @field:Resource
    private lateinit var ollamaChatModel: OllamaChatModel

    private val prompt = "你好，你是一个研发工程师，擅长解决技术类问题。"
    private val model = "qwen3:0.6b"
    private val apiKey = "app-46gHBiqUb5jqAHl9TDWwnRZ8"
    private val baseUrl = "http://localhost/v1"
    private val objectMapper = ObjectMapper()

    @RequestMapping("/")
    @ResponseBody
    fun index(): String = "task-pilot ai executor running."

    /**
     * ChatClient 简单调用。
     */
    @GetMapping("/chat/simple")
    @ResponseBody
    fun simpleChat(
        @RequestParam(value = "input", required = false, defaultValue = "介绍你自己") input: String
    ): String {
        val responseContent = buildChatClient()
            .prompt(prompt)
            .user(input)
            .call()
            .content()
            .orEmpty()

        logger.info("result: {}", responseContent)
        return responseContent
    }

    /**
     * ChatClient 流式调用。
     */
    @GetMapping("/chat/stream")
    fun streamChat(
        response: HttpServletResponse,
        @RequestParam(value = "input", required = false, defaultValue = "介绍你自己") input: String
    ): Flux<String> {
        response.characterEncoding = "UTF-8"
        return buildChatClient()
            .prompt(prompt)
            .user(input)
            .stream()
            .content()
    }

    @GetMapping("/dify/simple")
    @ResponseBody
    fun difySimple(@RequestParam(required = false, value = "input") input: String?): String {
        val inputs = HashMap<String, Any>()
        input?.let { inputs["input"] = it }

        val request = WorkflowRunRequest.builder()
            .inputs(inputs)
            .responseMode(ResponseMode.BLOCKING)
            .user("user-123")
            .build()

        val workflowClient = DifyClientFactory.createWorkflowClient(baseUrl, apiKey)
        val workflowResponse = workflowClient.runWorkflow(request)
        return writeToJson(workflowResponse.data.outputs)
    }

    /**
     * 通过 Flux 包装 Dify 的回调接口，便于示例端直接观察流式输出。
     */
    @GetMapping("/dify/stream")
    fun difyStream(@RequestParam(required = false, value = "input") input: String?): Flux<String> {
        val inputs = HashMap<String, Any>()
        input?.let { inputs["input"] = it }

        val request = WorkflowRunRequest.builder()
            .inputs(inputs)
            .responseMode(ResponseMode.STREAMING)
            .user("user-123")
            .build()

        val workflowClient = DifyClientFactory.createWorkflowClient(baseUrl, apiKey)
        return Flux.create { sink ->
            try {
                workflowClient.runWorkflowStream(request, object : WorkflowStreamCallback {
                    override fun onWorkflowStarted(event: WorkflowStartedEvent) {
                        sink.next("工作流开始: ${writeToJson(event.data)}")
                    }

                    override fun onNodeStarted(event: NodeStartedEvent) {
                        sink.next("节点开始: ${writeToJson(event.data)}")
                    }

                    override fun onNodeFinished(event: NodeFinishedEvent) {
                        sink.next("节点完成: ${writeToJson(event.data.outputs)}")
                    }

                    override fun onWorkflowFinished(event: WorkflowFinishedEvent) {
                        sink.next("工作流完成: ${writeToJson(event.data.outputs)}")
                        sink.complete()
                    }

                    override fun onError(event: ErrorEvent) {
                        sink.error(RuntimeException(event.message))
                    }

                    override fun onException(throwable: Throwable) {
                        sink.error(throwable)
                    }
                })
            } catch (e: Exception) {
                sink.error(RuntimeException(e))
            }
        }
    }

    /**
     * 统一封装聊天客户端构建，避免简单调用和流式调用在配置上出现偏差。
     */
    private fun buildChatClient(): ChatClient =
        ChatClient.builder(ollamaChatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
            .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
            .defaultOptions(OllamaChatOptions.builder().model(model).build())
            .build()

    private fun writeToJson(obj: Any?): String {
        if (obj == null) {
            return "null"
        }
        return try {
            objectMapper.writeValueAsString(obj)
        } catch (_: JsonProcessingException) {
            obj.toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexController::class.java)
    }
}
