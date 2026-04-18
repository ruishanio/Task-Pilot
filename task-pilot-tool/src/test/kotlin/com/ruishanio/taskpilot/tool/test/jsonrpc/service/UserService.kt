package com.ruishanio.taskpilot.tool.test.jsonrpc.service

import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.ResultDTO
import com.ruishanio.taskpilot.tool.test.jsonrpc.service.model.UserDTO

/**
 * JsonRpc 测试服务接口。
 */
interface UserService {
    fun createUser(userDTO: UserDTO?): ResultDTO?

    fun updateUser(name: String?, age: Int?): ResultDTO?

    fun loadUser(name: String?): UserDTO?

    fun queryUserByAge(): List<UserDTO>?

    fun refresh()

    fun load(name: String?): Response<UserDTO>?
}
