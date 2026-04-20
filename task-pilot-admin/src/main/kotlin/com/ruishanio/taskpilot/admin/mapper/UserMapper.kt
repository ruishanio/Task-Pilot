package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.User
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 管理端用户 Mapper。
 */
@Mapper
interface UserMapper {
    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("username") username: String?,
        @Param("role") role: Int
    ): List<User>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("username") username: String?,
        @Param("role") role: Int
    ): Int

    fun loadByUserName(@Param("username") username: String?): User?

    fun loadById(@Param("id") id: Int): User?

    fun save(user: User): Int

    fun update(user: User): Int

    fun delete(@Param("id") id: Int): Int

    fun updateToken(@Param("id") id: Int, @Param("token") token: String?): Int
}
