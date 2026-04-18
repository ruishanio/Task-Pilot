package com.ruishanio.taskpilot.admin.scheduler.trigger

import com.ruishanio.taskpilot.admin.mapper.TaskPilotGroupMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotInfoMapper
import com.ruishanio.taskpilot.admin.mapper.TaskPilotLogMapper
import com.ruishanio.taskpilot.admin.model.TaskPilotGroup
import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.model.TaskPilotLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.core.constant.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.error.ThrowableTool
import com.ruishanio.taskpilot.tool.http.IPTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.Date

/**
 * 任务触发器。
 *
 * 继续保留“先记日志、再路由、后远程触发”的顺序，保证远程执行失败时也能回溯完整触发上下文。
 */
@Component
class JobTrigger {
    @Resource
    private lateinit var taskPilotInfoMapper: TaskPilotInfoMapper

    @Resource
    private lateinit var taskPilotGroupMapper: TaskPilotGroupMapper

    @Resource
    private lateinit var taskPilotLogMapper: TaskPilotLogMapper

    /**
     * 统一处理手动触发、Cron 触发、失火补偿和失败重试等入口。
     */
    fun trigger(
        jobId: Int,
        triggerType: TriggerTypeEnum,
        failRetryCount: Int,
        executorShardingParam: String?,
        executorParam: String?,
        addressList: String?
    ) {
        val jobInfo = taskPilotInfoMapper.loadById(jobId)
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>>> 触发任务失败，jobId 无效，jobId={}", jobId)
            return
        }
        if (executorParam != null) {
            jobInfo.executorParam = executorParam
        }
        val finalFailRetryCount = if (failRetryCount >= 0) failRetryCount else jobInfo.executorFailRetryCount
        val group = taskPilotGroupMapper.load(jobInfo.jobGroup) ?: return

        if (StringTool.isNotBlank(addressList)) {
            group.addressType = 1
            group.addressList = addressList!!.trim()
        }

        var shardingParam: IntArray? = null
        val triggerTime = Date()
        if (executorShardingParam != null) {
            val shardingArr = executorShardingParam.split("/")
            if (shardingArr.size == 2 && StringTool.isNumeric(shardingArr[0]) && StringTool.isNumeric(shardingArr[1])) {
                shardingParam = intArrayOf(shardingArr[0].toInt(), shardingArr[1].toInt())
            }
        }

        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum.match(jobInfo.executorRouteStrategy, null) &&
            !group.registryList.isNullOrEmpty() &&
            shardingParam == null
        ) {
            for (i in group.registryList!!.indices) {
                processTrigger(group, jobInfo, finalFailRetryCount, triggerType, triggerTime, i, group.registryList!!.size)
            }
        } else {
            if (shardingParam == null) {
                shardingParam = intArrayOf(0, 1)
            }
            processTrigger(group, jobInfo, finalFailRetryCount, triggerType, triggerTime, shardingParam[0], shardingParam[1])
        }
    }

    /**
     * 单次触发过程中会生成独立日志记录，并把路由与执行结果汇总到 triggerMsg。
     */
    private fun processTrigger(
        group: TaskPilotGroup,
        jobInfo: TaskPilotInfo,
        finalFailRetryCount: Int,
        triggerType: TriggerTypeEnum,
        triggerTime: Date,
        index: Int,
        total: Int
    ) {
        val blockStrategy = ExecutorBlockStrategyEnum.match(
            jobInfo.executorBlockStrategy,
            ExecutorBlockStrategyEnum.SERIAL_EXECUTION
        ) ?: ExecutorBlockStrategyEnum.SERIAL_EXECUTION
        val executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.executorRouteStrategy, null)
            ?: ExecutorRouteStrategyEnum.FIRST
        val shardingParam = if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
            "$index/$total"
        } else {
            null
        }

        val jobLog = TaskPilotLog().apply {
            jobGroup = jobInfo.jobGroup
            jobId = jobInfo.id
            this.triggerTime = triggerTime
        }
        taskPilotLogMapper.save(jobLog)
        logger.debug(">>>>>>>>>>> task-pilot 任务触发开始，logId={}", jobLog.id)

        val triggerParam = TriggerRequest().apply {
            this.jobId = jobInfo.id
            executorHandler = jobInfo.executorHandler
            executorParams = jobInfo.executorParam
            executorBlockStrategy = jobInfo.executorBlockStrategy
            executorTimeout = jobInfo.executorTimeout
            logId = jobLog.id
            logDateTime = jobLog.triggerTime!!.time
            glueType = jobInfo.glueType
            glueSource = jobInfo.glueSource
            glueUpdatetime = jobInfo.glueUpdatetime?.time ?: 0L
            broadcastIndex = index
            broadcastTotal = total
        }

        var address: String? = null
        var routeAddressResult: Response<String>? = null
        if (!group.registryList.isNullOrEmpty()) {
            if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
                address = if (index < group.registryList!!.size) group.registryList!![index] else group.registryList!![0]
            } else {
                routeAddressResult = executorRouteStrategyEnum.router?.route(triggerParam, group.registryList!!)
                if (routeAddressResult?.isSuccess == true) {
                    address = routeAddressResult.data
                }
            }
        } else {
            routeAddressResult = Response.of(TaskPilotContext.HANDLE_CODE_FAIL, I18nUtil.getString("jobconf_trigger_address_empty"))
        }

        val triggerResult = if (address != null) {
            doTrigger(triggerParam, address)
        } else {
            Response.of(TaskPilotContext.HANDLE_CODE_FAIL, "地址路由失败。")
        }

        val triggerMsgSb = StringBuilder()
        triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：").append(triggerType.title)
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress")).append("：").append(IPTool.getIp())
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype")).append("：")
            .append(
                if (group.addressType == 0) {
                    I18nUtil.getString("jobgroup_field_addressType_0")
                } else {
                    I18nUtil.getString("jobgroup_field_addressType_1")
                }
            )
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress")).append("：").append(group.registryList)
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy")).append("：")
            .append(executorRouteStrategyEnum.title)
        if (shardingParam != null) {
            triggerMsgSb.append("(").append(shardingParam).append(")")
        }
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy")).append("：").append(blockStrategy.title)
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：").append(jobInfo.executorTimeout)
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount")).append("：").append(finalFailRetryCount)

        triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>")
            .append(I18nUtil.getString("jobconf_trigger_run"))
            .append("<<<<<<<<<<< </span><br>")
        triggerMsgSb.append("<br>").append(I18nUtil.getString("joblog_field_executorAddress")).append("：")
        when {
            StringTool.isNotBlank(address) -> triggerMsgSb.append(address)
            routeAddressResult != null && !routeAddressResult.isSuccess && routeAddressResult.msg != null ->
                triggerMsgSb.append("地址路由失败，").append(routeAddressResult.msg)
            else -> triggerMsgSb.append("地址路由失败。")
        }
        if (StringTool.isNotBlank(jobInfo.executorHandler)) {
            triggerMsgSb.append("<br>").append("任务处理器").append("：").append(jobInfo.executorHandler)
        }
        triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorparam")).append("：").append(jobInfo.executorParam)
        triggerMsgSb.append("<br>").append(I18nUtil.getString("joblog_field_triggerMsg")).append("：")
        when {
            triggerResult.isSuccess -> triggerMsgSb.append("成功")
            triggerResult.msg != null -> triggerMsgSb.append("异常，").append(triggerResult.msg)
            else -> triggerMsgSb.append("失败")
        }

        jobLog.executorAddress = address
        jobLog.executorHandler = jobInfo.executorHandler
        jobLog.executorParam = jobInfo.executorParam
        jobLog.executorShardingParam = shardingParam
        jobLog.executorFailRetryCount = finalFailRetryCount
        jobLog.triggerCode = triggerResult.code
        jobLog.triggerMsg = triggerMsgSb.toString()
        taskPilotLogMapper.updateTriggerInfo(jobLog)

        logger.debug(">>>>>>>>>>> task-pilot 任务触发结束，logId={}", jobLog.id)
    }

    /**
     * 远程执行器调用失败时把异常文本直接落到响应里，便于触发日志展示。
     */
    private fun doTrigger(triggerParam: TriggerRequest, address: String): Response<String> {
        return try {
            val executorBiz: ExecutorBiz = TaskPilotAdminBootstrap.getExecutorBiz(address)!!
            val runResult = executorBiz.run(triggerParam)

            val runResultSb = StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：")
            runResultSb.append("<br>地址：").append(address)
            runResultSb.append("<br>状态码：").append(runResult.code)
            runResultSb.append("<br>消息：").append(runResult.msg)
            runResult.msg = runResultSb.toString()
            runResult
        } catch (e: Exception) {
            logger.error(">>>>>>>>>>> task-pilot 触发任务时发生异常，请检查执行器[{}]是否在线。", address, e)
            Response.of(TaskPilotContext.HANDLE_CODE_FAIL, ThrowableTool.toString(e))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobTrigger::class.java)
    }
}
