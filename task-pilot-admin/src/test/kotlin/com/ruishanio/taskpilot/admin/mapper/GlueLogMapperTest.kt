package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.GlueLog
import jakarta.annotation.Resource
import java.util.Date
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 覆盖 GLUE 日志 Mapper 的增删查。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlueLogMapperTest {
    @field:Resource
    private lateinit var glueLogMapper: GlueLogMapper

    @Test
    fun test() {
        val logGlue =
            GlueLog().apply {
                jobId = 1
                glueType = "1"
                glueSource = "1"
                glueRemark = "1"
                addTime = Date()
                updateTime = Date()
            }

        val ret = glueLogMapper.save(logGlue)
        val list = glueLogMapper.findByJobId(1)
        val ret2 = glueLogMapper.removeOld(1, 1)
        val ret3 = glueLogMapper.deleteByJobId(1)

        println("ret=$ret, list=$list, ret2=$ret2, ret3=$ret3")
    }
}
