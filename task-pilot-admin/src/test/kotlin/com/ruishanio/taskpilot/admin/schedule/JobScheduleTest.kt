package com.ruishanio.taskpilot.admin.schedule

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.tool.core.DateTool
import java.util.Date
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition

/**
 * 覆盖调度锁的并发竞争场景。
 */
@SpringBootTest
class JobScheduleTest {
    @Test
    @Throws(InterruptedException::class)
    fun test() {
        repeat(10) { index ->
            Thread {
                lockTest("threadName-$index")
            }.start()
        }

        TimeUnit.MINUTES.sleep(10)
    }

    /**
     * 保留显式事务包裹锁查询的写法，用于复现数据库 `for update` 竞争行为。
     */
    private fun lockTest(threadName: String) {
        val transactionStatus: TransactionStatus =
            TaskPilotAdminBootstrap.instance.transactionManager.getTransaction(DefaultTransactionDefinition())
        try {
            val lockedRecord = TaskPilotAdminBootstrap.instance.taskPilotLockMapper.scheduleLock()
            logger.info("{} : lockedRecord={}", threadName, lockedRecord)
            logger.info("{} : start at {}", threadName, DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS"))
            TimeUnit.MILLISECONDS.sleep(500)
            logger.info("{} : end at {}", threadName, DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS"))
        } catch (e: Throwable) {
            logger.error("error: ", e)
        } finally {
            logger.info("{} : commit at {}", threadName, DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS"))
            TaskPilotAdminBootstrap.instance.transactionManager.commit(transactionStatus)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobScheduleTest::class.java)
    }
}
