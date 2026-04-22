package com.ruishanio.taskpilot.admin.controller

import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import com.ruishanio.taskpilot.admin.web.ManageSpaForwardFilter

/**
 * 提供基于 WebApplicationContext 的 MockMvc 基类。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
open class AbstractSpringMvcTest {
    @Autowired
    private lateinit var applicationContext: WebApplicationContext

    @Autowired(required = false)
    private var springSecurityFilterChain: Filter? = null

    @Autowired(required = false)
    private var manageSpaForwardFilter: ManageSpaForwardFilter? = null

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        // 显式挂上 springSecurityFilterChain，确保测试走到真实 JWT 过滤器和方法级授权。
        val builder: DefaultMockMvcBuilder = MockMvcBuilders.webAppContextSetup(applicationContext)
        val securityFilterChain = springSecurityFilterChain
        if (securityFilterChain != null) {
            builder.addFilters<DefaultMockMvcBuilder>(securityFilterChain)
        }
        val spaForwardFilter = manageSpaForwardFilter
        if (spaForwardFilter != null) {
            builder.addFilters<DefaultMockMvcBuilder>(spaForwardFilter)
        }
        mockMvc = builder.build()
    }
}
