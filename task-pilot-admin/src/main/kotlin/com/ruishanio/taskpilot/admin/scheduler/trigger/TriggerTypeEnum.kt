package com.ruishanio.taskpilot.admin.scheduler.trigger

import com.ruishanio.taskpilot.admin.util.I18nUtil

/**
 * 任务触发来源枚举。
 */
enum class TriggerTypeEnum(val title: String) {
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"))
}
