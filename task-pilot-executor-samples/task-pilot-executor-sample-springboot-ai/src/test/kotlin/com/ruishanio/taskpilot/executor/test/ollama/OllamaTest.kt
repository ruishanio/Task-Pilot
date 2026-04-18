package com.ruishanio.taskpilot.executor.test.ollama

import com.ruishanio.taskpilot.core.executor.impl.TaskPilotSpringExecutor
import jakarta.annotation.Resource
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Flux

/**
 * Ollama 样例测试，覆盖同步和流式调用两个入口。
 */
@SpringBootTest
class OllamaTest {
    @field:MockitoBean
    private lateinit var taskPilotSpringExecutor: TaskPilotSpringExecutor

    @field:Resource
    private lateinit var ollamaChatModel: OllamaChatModel

    @Test
    fun chatTest() {
        val model = "qwen3:0.6b"
        val prompt = "背景说明：你是一个研发工程师，擅长解决技术类问题。"
        val input = "请写一个java程序，实现一个方法，输入一个字符串，返回字符串的长度。"

        val ollamaChatClient =
            ChatClient
                .builder(ollamaChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .defaultOptions(OllamaChatOptions.builder().model(model).build())
                .build()

        val response =
            ollamaChatClient
                .prompt(prompt)
                .user(input)
                .call()
                .content()

        logger.info("input: {}", input)
        logger.info("response: {}", response)
    }

    @Test
    @Throws(InterruptedException::class)
    fun chatStreamTest() {
        val model = "qwen3:0.6b"
        val prompt = "背景说明：你是一个研发工程师，擅长解决技术类问题。"
        val input = "请写一个java程序，实现一个方法，输入一个字符串，返回字符串的长度。"

        val ollamaChatClient =
            ChatClient
                .builder(ollamaChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .defaultOptions(OllamaChatOptions.builder().model(model).build())
                .build()

        logger.info("input: {}", input)
        val flux: Flux<String> =
            ollamaChatClient
                .prompt(prompt)
                .user(input)
                .stream()
                .content()

        flux.subscribe(
            { data -> println("Received: $data") },
            { error -> System.err.println("Error: $error") },
            { println("Completed") }
        )

        TimeUnit.SECONDS.sleep(10)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OllamaTest::class.java)
    }
}
