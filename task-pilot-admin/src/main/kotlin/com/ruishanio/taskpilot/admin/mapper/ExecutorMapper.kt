package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.Executor
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 执行器分组 Mapper。
 */
@Mapper
interface ExecutorMapper {
    fun findAll(): List<Executor>

    fun findByAddressType(@Param("addressType") addressType: Int): List<Executor>

    fun save(executor: Executor): Int

    fun update(executor: Executor): Int

    fun remove(@Param("id") id: Int): Int

    fun load(@Param("id") id: Int): Executor?

    /**
     * 按执行器 appname 查询分组。
     *
     * 自动注册场景依赖该查询做幂等判断，避免重复创建执行器分组。
     */
    fun loadByAppname(@Param("appname") appname: String?): Executor?

    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("appname") appname: String?,
        @Param("title") title: String?
    ): List<Executor>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("appname") appname: String?,
        @Param("title") title: String?
    ): Int
}
