package com.ruishanio.taskpilot.tool.concurrent

import com.ruishanio.taskpilot.tool.core.StringTool
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * 生产者消费者队列。
 * 继续维持阻塞队列 + 固定消费线程池的实现，避免把批量消费和停止排空语义改掉。
 */
class MessageQueue<T>(
    private val name: String,
    queueLength: Int,
    private val consumer: MessageConsumer<T>,
    consumerCount: Int,
    private val consumeBatchSize: Int,
) {
    private val messageQueue: LinkedBlockingQueue<T>
    private val consumerExecutor: ExecutorService

    @Volatile
    private var isRunning: Boolean = true

    constructor(name: String, consumer: MessageConsumer<T>) : this(name, Integer.MAX_VALUE, consumer, 3, 1)

    constructor(name: String, consumer: MessageConsumer<T>, consumeBatchSize: Int) :
        this(name, Integer.MAX_VALUE, consumer, 3, consumeBatchSize)

    constructor(name: String, consumer: MessageConsumer<T>, consumerCount: Int, consumeBatchSize: Int) :
        this(name, Integer.MAX_VALUE, consumer, consumerCount, consumeBatchSize)

    init {
        if (StringTool.isBlank(name)) {
            throw IllegalArgumentException("name is null")
        }
        if (queueLength < 1) {
            throw IllegalArgumentException("queueLength is invalid.")
        }
        if (consumerCount < 1) {
            throw IllegalArgumentException("consumerCount is invalid.")
        }
        if (consumeBatchSize < 1) {
            throw IllegalArgumentException("consumeBatchSize is invalid.")
        }

        messageQueue = LinkedBlockingQueue(queueLength)
        consumerExecutor = Executors.newFixedThreadPool(consumerCount)
        isRunning = true

        for (i in 0 until consumerCount) {
            consumerExecutor.submit {
                logger.debug(">>>>>>>>>>> ProducerConsumerQueue[name = {}] 消费线程[{}]已启动。", name, Thread.currentThread().name)
                while (isRunning || !messageQueue.isEmpty()) {
                    try {
                        val message = messageQueue.poll(3000, TimeUnit.MILLISECONDS)
                        if (message != null) {
                            val messageList = ArrayList<T>()
                            messageList.add(message)
                            if (consumeBatchSize > 1) {
                                messageQueue.drainTo(messageList, consumeBatchSize - 1)
                            }
                            consumer.consume(messageList)
                        }
                    } catch (e: Throwable) {
                        if (isRunning) {
                            logger.error(">>>>>>>>>>> ProducerConsumerQueue[name = {}] 消费线程[{}]执行异常。", name, Thread.currentThread().name, e)
                        }
                        if (!isRunning && e is InterruptedException) {
                            logger.error(">>>>>>>>>>> ProducerConsumerQueue[name = {}] 消费线程[{}]在停止过程中被中断，可能发生了停止超时。", name, Thread.currentThread().name)
                        }
                    }
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread { stop() })
        logger.info(
            ">>>>>>>>>>> ProducerConsumerQueue[name = {}] 已启动，配置：queueLength={}, consumerCount={}, consumeBatchSize={}",
            name,
            queueLength,
            consumerCount,
            consumeBatchSize,
        )
    }

    /**
     * 继续保留批量消费接口，避免把原有批量刷库/批量发送场景退化成逐条回调。
     */
    fun interface MessageConsumer<T> {
        fun consume(messages: kotlin.collections.List<T>)
    }

    fun produce(message: T?): Boolean {
        if (message == null || !isRunning) {
            return false
        }

        return try {
            val result = messageQueue.offer(message, 20, TimeUnit.MILLISECONDS)
            if (!result) {
                logger.warn(">>>>>>>>>>> ProducerConsumerQueue[name = {}] 消息入队失败，message:{}", name, message.toString())
            }
            result
        } catch (e: InterruptedException) {
            logger.warn(">>>>>>>>>>> ProducerConsumerQueue[name = {}] 消息入队时被中断，message:{}", name, message.toString(), e)
            false
        }
    }

    /**
     * 停止时继续先拒绝新消息，再等待队列排空和消费线程退出。
     */
    fun stop() {
        if (isRunning) {
            isRunning = false
            consumerExecutor.shutdown()
            try {
                if (!consumerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                consumerExecutor.shutdownNow()
                logger.error(">>>>>>>>>>> ProducerConsumerQueue[name = {}] 停止时发生异常。", name, e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MessageQueue::class.java)
    }
}
