package com.ruishanio.taskpilot.admin.service

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.tool.response.PageModel
import com.ruishanio.taskpilot.tool.response.Response
import java.util.Date

/**
 * 管理端任务服务接口。
 */
interface TaskPilotService {
    fun pageList(
        offset: Int,
        pagesize: Int,
        jobGroup: Int,
        triggerStatus: Int,
        taskName: String?,
        jobDesc: String?,
        executorHandler: String?,
        author: String?
    ): Response<PageModel<TaskInfo>>

    fun add(jobInfo: TaskInfo, loginInfo: LoginInfo): Response<String>

    fun update(jobInfo: TaskInfo, loginInfo: LoginInfo): Response<String>

    fun remove(id: Int, loginInfo: LoginInfo): Response<String>

    fun start(id: Int, loginInfo: LoginInfo): Response<String>

    fun stop(id: Int, loginInfo: LoginInfo): Response<String>

    fun trigger(loginInfo: LoginInfo, jobId: Int, executorParam: String?, addressList: String?): Response<String>

    fun dashboardInfo(): Map<String, Any>

    fun chartInfo(startDate: Date?, endDate: Date?): Response<Map<String, Any>>
}
