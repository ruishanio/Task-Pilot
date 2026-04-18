package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import jakarta.annotation.Resource
import java.util.Date
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖执行器分组 Mapper 的基础 CRUD。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskPilotGroupMapperTest {
    @field:Resource
    private lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Test
    fun test() {
        val list = taskPilotGroupMapper.findAll()
        val list2 = taskPilotGroupMapper.findByAddressType(0)

        val group =
            TaskPilotGroup().apply {
                appname = "setAppName"
                title = "setTitle"
                addressType = 0
                addressList = "setAddressList"
                updateTime = Date()
            }

        val ret = taskPilotGroupMapper.save(group)

        val group2 = taskPilotGroupMapper.load(group.id)!!
        group2.appname = "setAppName2"
        group2.title = "setTitle2"
        group2.addressType = 2
        group2.addressList = "setAddressList2"
        group2.updateTime = Date()

        val ret2 = taskPilotGroupMapper.update(group2)
        val ret3 = taskPilotGroupMapper.remove(group.id)

        println("list=$list, list2=$list2, ret=$ret, ret2=$ret2, ret3=$ret3")
    }
}
