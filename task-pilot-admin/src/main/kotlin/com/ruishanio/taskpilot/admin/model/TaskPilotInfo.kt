package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * 任务定义模型。
 */
data class TaskPilotInfo(
    var id: Int = 0,
    var jobGroup: Int = 0,
    var jobDesc: String? = null,
    var addTime: Date? = null,
    var updateTime: Date? = null,
    var author: String? = null,
    var alarmEmail: String? = null,
    var scheduleType: String? = null,
    var scheduleConf: String? = null,
    var misfireStrategy: String? = null,
    var executorRouteStrategy: String? = null,
    var executorHandler: String? = null,
    var executorParam: String? = null,
    var executorBlockStrategy: String? = null,
    var executorTimeout: Int = 0,
    var executorFailRetryCount: Int = 0,
    var glueType: String? = null,
    var glueSource: String? = null,
    var glueRemark: String? = null,
    var glueUpdatetime: Date? = null,
    var childJobId: String? = null,
    var triggerStatus: Int = 0,
    var triggerLastTime: Long = 0,
    var triggerNextTime: Long = 0
)
