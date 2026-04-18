package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.BeanTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * BeanTool 的映射与复制能力验证。
 */
class BeanToolTest {
    @Test
    fun copyProperties() {
        val userDTO = UserDTO("jack", 18)
        val userDTO2 = BeanTool.copyProperties(userDTO, User2DTO::class.java)
        val userDTO3 = BeanTool.copyProperties(userDTO, UserDTO::class.java)

        logger.info("userDTO: {}", userDTO)
        logger.info("userDTO2: {}", userDTO2)
        logger.info("userDTO3: {}", userDTO3)
    }

    @Test
    fun testBeanMapConvert() {
        val userDTO = UserDTO("jack", 18)
        val mapped = BeanTool.convertBeanFieldToMap(userDTO)
        logger.info("objectToPrimitive: {}", mapped)

        val restored = BeanTool.convertMapFieldToBean(mapped, UserDTO::class.java) as UserDTO
        logger.info("primitiveToObject: {}", restored)
    }

    @Test
    fun beanToMap() {
        val userDTO = UserDTO("jack", 18)
        val map = BeanTool.beanToMap(userDTO)
        logger.info("beanToMap: {}", map)

        val userDTO1 = BeanTool.mapToBean(map, UserDTO::class.java)
        logger.info("mapToBean: {}", userDTO1)
    }

    /**
     * 子类场景用于验证属性复制时的继承字段处理。
     */
    class User2DTO() : UserDTO() {
        var realName: String? = null

        constructor(name: String?, age: Int, realName: String?) : this() {
            this.name = name
            this.age = age
            this.realName = realName
        }

        override fun toString(): String = "User2DTO{realName='$realName', name='$name', age=$age}"
    }

    /**
     * 保留可写属性和无参构造，兼容 BeanTool 的反射赋值路径。
     */
    open class UserDTO() {
        var name: String? = null
        var age: Int = 0

        constructor(name: String?, age: Int) : this() {
            this.name = name
            this.age = age
        }

        override fun toString(): String = "UserDTO{name='$name', age=$age}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BeanToolTest::class.java)
    }
}
