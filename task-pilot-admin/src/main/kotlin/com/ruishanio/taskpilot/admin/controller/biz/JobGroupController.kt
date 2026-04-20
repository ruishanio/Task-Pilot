package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.ExecutorMapper
import com.ruishanio.taskpilot.admin.model.Executor
import com.ruishanio.taskpilot.admin.mapper.RegistryMapper
import com.ruishanio.taskpilot.admin.mapper.TaskInfoMapper
import com.ruishanio.taskpilot.admin.model.Registry
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.ArrayList
import java.util.Date
import java.util.HashMap

/**
 * 执行器分组管理控制器。
 */
@Controller
@RequestMapping(ManageRoute.API_MANAGE_JOBGROUP)
class JobGroupController {
    @Resource
    lateinit var taskInfoMapper: TaskInfoMapper

    @Resource
    lateinit var executorMapper: ExecutorMapper

    @Resource
    private lateinit var registryMapper: RegistryMapper

    @RequestMapping("/pageList")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun pageList(
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        appname: String?,
        title: String?
    ): Response<PageModel<Executor>> {
        val list = executorMapper.pageList(offset, pagesize, appname, title)
        val listCount = executorMapper.pageListCount(offset, pagesize, appname, title)
        val pageModel = PageModel<Executor>()
        pageModel.data = list
        pageModel.total = listCount
        return Response.ofSuccess(pageModel)
    }

    /**
     * 自动注册分组和手动录入分组共用同一表单，但地址校验规则不同。
     */
    @RequestMapping("/insert")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun insert(executor: Executor): Response<String> {
        val validResult = validGroup(executor, false)
        if (!validResult.isSuccess) {
            return validResult
        }

        executor.updateTime = Date()
        val ret = executorMapper.save(executor)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail()
    }

    @RequestMapping("/update")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun update(executor: Executor): Response<String> {
        val validResult = validGroup(executor, true)
        if (!validResult.isSuccess) {
            return validResult
        }

        executor.updateTime = Date()
        val ret = executorMapper.update(executor)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail()
    }

    @RequestMapping("/delete")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun delete(@RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail("请选择一条数据")
        }
        val id = ids[0]

        val executor = executorMapper.load(id) ?: return Response.ofSuccess()
        val count = taskInfoMapper.pageListCount(0, 10, id, -1, null, null, null, null)
        if (count > 0) {
            return Response.ofFail("拒绝删除，该执行器使用中")
        }

        val allList = executorMapper.findAll()
        if (allList.size == 1) {
            return Response.ofFail("拒绝删除, 系统至少保留一个执行器")
        }

        val ret = executorMapper.remove(id)
        registryMapper.removeByRegistryGroupAndKey(RegistType.EXECUTOR.name, executor.appname)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail()
    }

    @RequestMapping("/loadById")
    @ResponseBody
    fun loadById(@RequestParam("id") id: Int): Response<Executor> {
        val jobGroup = executorMapper.load(id)
        return if (jobGroup != null) Response.ofSuccess(jobGroup) else Response.ofFail()
    }

    /**
     * 手动录入地址要校验 URL 形式，自动注册则直接按注册表回填。
     */
    private fun validGroup(executor: Executor, isUpdate: Boolean): Response<String> {
        executor.appname = executor.appname?.trim()
        executor.title = executor.title?.trim()
        if (StringTool.isBlank(executor.appname)) {
            return Response.ofFail("请输入AppName")
        }
        if (executor.appname!!.length !in 4..64) {
            return Response.ofFail("AppName长度限制为4~64")
        }
        if (executor.appname!!.contains(">") || executor.appname!!.contains("<")) {
            return Response.ofFail("AppName非法")
        }
        if (StringTool.isBlank(executor.title)) {
            return Response.ofFail("请输入名称")
        }
        if (executor.title!!.contains(">") || executor.title!!.contains("<")) {
            return Response.ofFail("名称非法")
        }

        val existsExecutor = executorMapper.loadByAppname(executor.appname)
        if (existsExecutor != null && (!isUpdate || existsExecutor.id != executor.id)) {
            return Response.ofFail("AppName重复")
        }

        if (isUpdate && executor.addressType == 0) {
            val registryList = findRegistryByAppName(executor.appname)
            executor.addressList = if (CollectionTool.isNotEmpty(registryList)) {
                val sortedRegistryList = registryList!!.sorted()
                sortedRegistryList.joinToString(",")
            } else {
                null
            }
        } else if (executor.addressType != 0) {
            if (StringTool.isBlank(executor.addressList)) {
                return Response.ofFail("手动录入注册方式，机器地址不可为空")
            }
            for (item in executor.addressList!!.split(",")) {
                if (StringTool.isBlank(item)) {
                    return Response.ofFail("机器地址格式非法")
                }
                if (!(HttpTool.isHttp(item) || HttpTool.isHttps(item))) {
                    return Response.ofFail("机器地址格式非法[2]")
                }
            }
        }
        return Response.ofSuccess()
    }

    /**
     * 自动注册分组更新时从注册表实时拼出最新地址列表。
     */
    private fun findRegistryByAppName(appnameParam: String?): MutableList<String>? {
        val appAddressMap = HashMap<String, MutableList<String>>()
        val list: List<Registry> = registryMapper.findAll(Const.DEAD_TIMEOUT, Date())
        if (CollectionTool.isNotEmpty(list)) {
            for (item in list) {
                if (RegistType.EXECUTOR.name != item.registryGroup) {
                    continue
                }
                val appname = item.registryKey ?: continue
                val registryList = appAddressMap.computeIfAbsent(appname) { ArrayList() }
                if (!registryList.contains(item.registryValue)) {
                    registryList.add(item.registryValue!!)
                }
            }
        }
        return appAddressMap[appnameParam]
    }
}
