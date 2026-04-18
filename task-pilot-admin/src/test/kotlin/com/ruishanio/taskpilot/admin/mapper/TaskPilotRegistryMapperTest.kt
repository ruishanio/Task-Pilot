package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotRegistry
import com.ruishanio.taskpilot.core.constant.RegistType
import jakarta.annotation.Resource
import java.util.Date
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖注册中心 Mapper 的保存、查询和并发更新场景。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskPilotRegistryMapperTest {
    @field:Resource
    private lateinit var taskPilotRegistryMapper: TaskPilotRegistryMapper

    @Test
    fun test() {
        val ret = taskPilotRegistryMapper.registrySaveOrUpdate(RegistType.EXECUTOR.name, "task-pilot-executor-z1", "v1", Date())
        val list: List<TaskPilotRegistry> = taskPilotRegistryMapper.findAll(1, Date())
        val ret2 = taskPilotRegistryMapper.removeDead(listOf(1))

        println("ret=$ret, list=$list, ret2=$ret2")
    }

    @Test
    @Throws(InterruptedException::class)
    fun test2() {
        repeat(100) {
            Thread {
                val ret = taskPilotRegistryMapper.registrySaveOrUpdate("g1", "k1", "v1", Date())
                println(ret)
            }.start()
        }

        TimeUnit.SECONDS.sleep(10)
    }
}
