package com.ruishanio.taskpilot.tool.test.jsonrpc.service.impl

import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.UserService
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.ResultDTO
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.UserDTO
import kotlin.random.Random

/**
 * JsonRpc 测试服务实现。
 */
class UserServiceImpl : UserService {
    override fun createUser(userDTO: UserDTO?): ResultDTO {
        println("createUser: $userDTO")
        return ResultDTO(true, "createUser success")
    }

    override fun updateUser(name: String?, age: Int?): ResultDTO {
        println("updateUser: name: $name, age: $age")
        return ResultDTO(true, "updateUser success")
    }

    override fun loadUser(name: String?): UserDTO = UserDTO("${name}(success)", Random.nextInt(28))

    override fun queryUserByAge(): List<UserDTO> =
        (0 until 3).map { UserDTO("user(success)$it", 17 + it) }

    override fun refresh() {
        println("refresh")
    }

    override fun load(name: String?): Response<UserDTO> =
        Response.ofSuccess(UserDTO("${name}(success)", Random.nextInt(28)))
}
