package com.ruishanio.taskpilot.admin.controller.biz

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.TaskPilotGroupMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotRegistryMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.admin.model.TaskPilotRegistry
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.constant.RegistType
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.HashMap

/**
 * 执行器分组管理控制器。
 */
@Controller
@RequestMapping("/jobgroup")
class JobGroupController {
    @Resource
    lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Resource
    lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Resource
    private lateinit var taskPilotRegistryMapper: TaskPilotRegistryMapper

    @RequestMapping
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun index(model: Model): String = "biz/group.list"

    @RequestMapping("/pageList")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun pageList(
        @RequestParam(required = false, defaultValue = "0") offset: Int,
        @RequestParam(required = false, defaultValue = "10") pagesize: Int,
        appname: String?,
        title: String?
    ): Response<PageModel<TaskPilotGroup>> {
        val list = taskPilotGroupMapper.pageList(offset, pagesize, appname, title)
        val listCount = taskPilotGroupMapper.pageListCount(offset, pagesize, appname, title)
        val pageModel = PageModel<TaskPilotGroup>()
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
    fun insert(taskPilotGroup: TaskPilotGroup): Response<String> {
        val validResult = validGroup(taskPilotGroup, false)
        if (!validResult.isSuccess) {
            return validResult
        }

        taskPilotGroup.updateTime = Date()
        val ret = taskPilotGroupMapper.save(taskPilotGroup)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail()
    }

    @RequestMapping("/update")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun update(taskPilotGroup: TaskPilotGroup): Response<String> {
        val validResult = validGroup(taskPilotGroup, true)
        if (!validResult.isSuccess) {
            return validResult
        }

        taskPilotGroup.updateTime = Date()
        val ret = taskPilotGroupMapper.update(taskPilotGroup)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail()
    }

    @RequestMapping("/delete")
    @ResponseBody
    @TaskPilotAuth(role = Consts.ADMIN_ROLE)
    fun delete(@RequestParam("ids[]") ids: List<Int>): Response<String> {
        if (CollectionTool.isEmpty(ids) || ids.size != 1) {
            return Response.ofFail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("system_one") + I18nUtil.getString("system_data"))
        }
        val id = ids[0]

        val taskPilotGroup = taskPilotGroupMapper.load(id) ?: return Response.ofSuccess()
        val count = taskPilotInfoMapper.pageListCount(0, 10, id, -1, null, null, null)
        if (count > 0) {
            return Response.ofFail(I18nUtil.getString("jobgroup_del_limit_0"))
        }

        val allList = taskPilotGroupMapper.findAll()
        if (allList.size == 1) {
            return Response.ofFail(I18nUtil.getString("jobgroup_del_limit_1"))
        }

        val ret = taskPilotGroupMapper.remove(id)
        taskPilotRegistryMapper.removeByRegistryGroupAndKey(RegistType.EXECUTOR.name, taskPilotGroup.appname)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail()
    }

    @RequestMapping("/loadById")
    @ResponseBody
    fun loadById(@RequestParam("id") id: Int): Response<TaskPilotGroup> {
        val jobGroup = taskPilotGroupMapper.load(id)
        return if (jobGroup != null) Response.ofSuccess(jobGroup) else Response.ofFail()
    }

    /**
     * 手动录入地址要校验 URL 形式，自动注册则直接按注册表回填。
     */
    private fun validGroup(taskPilotGroup: TaskPilotGroup, isUpdate: Boolean): Response<String> {
        if (StringTool.isBlank(taskPilotGroup.appname)) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + "AppName")
        }
        if (taskPilotGroup.appname!!.length !in 4..64) {
            return Response.ofFail(I18nUtil.getString("jobgroup_field_appname_length"))
        }
        if (taskPilotGroup.appname!!.contains(">") || taskPilotGroup.appname!!.contains("<")) {
            return Response.ofFail("AppName" + I18nUtil.getString("system_unvalid"))
        }
        if (StringTool.isBlank(taskPilotGroup.title)) {
            return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title"))
        }
        if (taskPilotGroup.title!!.contains(">") || taskPilotGroup.title!!.contains("<")) {
            return Response.ofFail(I18nUtil.getString("jobgroup_field_title") + I18nUtil.getString("system_unvalid"))
        }

        if (isUpdate && taskPilotGroup.addressType == 0) {
            val registryList = findRegistryByAppName(taskPilotGroup.appname)
            taskPilotGroup.addressList = if (CollectionTool.isNotEmpty(registryList)) {
                val sortedRegistryList = registryList!!.sorted()
                sortedRegistryList.joinToString(",")
            } else {
                null
            }
        } else if (taskPilotGroup.addressType != 0) {
            if (StringTool.isBlank(taskPilotGroup.addressList)) {
                return Response.ofFail(I18nUtil.getString("jobgroup_field_addressType_limit"))
            }
            for (item in taskPilotGroup.addressList!!.split(",")) {
                if (StringTool.isBlank(item)) {
                    return Response.ofFail(I18nUtil.getString("jobgroup_field_registryList_unvalid"))
                }
                if (!(HttpTool.isHttp(item) || HttpTool.isHttps(item))) {
                    return Response.ofFail(I18nUtil.getString("jobgroup_field_registryList_unvalid") + "[2]")
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
        val list: List<TaskPilotRegistry> = taskPilotRegistryMapper.findAll(Const.DEAD_TIMEOUT, Date())
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
