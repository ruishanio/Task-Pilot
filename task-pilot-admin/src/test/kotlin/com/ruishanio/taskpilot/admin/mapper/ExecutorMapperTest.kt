package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.Executor
import jakarta.annotation.Resource
import java.util.Date
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖执行器分组 Mapper 的基础 CRUD。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExecutorMapperTest {
    @field:Resource
    private lateinit var executorMapper: ExecutorMapper

    @Test
    fun test() {
        val list = executorMapper.findAll()
        val list2 = executorMapper.findByAddressType(0)

        val group =
            Executor().apply {
                appname = "setAppName"
                title = "setTitle"
                addressType = 0
                addressList = "setAddressList"
                updateTime = Date()
            }

        val ret = executorMapper.save(group)

        val group2 = executorMapper.load(group.id)!!
        group2.appname = "setAppName2"
        group2.title = "setTitle2"
        group2.addressType = 2
        group2.addressList = "setAddressList2"
        group2.updateTime = Date()

        val ret2 = executorMapper.update(group2)
        val ret3 = executorMapper.remove(group.id)

        println("list=$list, list2=$list2, ret=$ret, ret2=$ret2, ret3=$ret3")
    }
}
