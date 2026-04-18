package com.ruishanio.taskpilot.tool.test.excel.model

import com.ruishanio.taskpilot.tool.excel.annotation.ExcelField
import com.ruishanio.taskpilot.tool.excel.annotation.ExcelSheet
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import java.util.Date

/**
 * Excel 商户测试模型。
 */
@ExcelSheet(name = "商户列表", headColor = IndexedColors.LIGHT_GREEN)
class ShopDTO(
    @field:ExcelField(name = "是否VIP商户")
    var vip: Boolean = false,
    @field:ExcelField(name = "商户名称", align = HorizontalAlignment.CENTER)
    var shopName: String? = null,
    @field:ExcelField(name = "分店数量")
    var branchNum: Short = 0,
    @field:ExcelField(name = "商户ID")
    var shopId: Int = 0,
    @field:ExcelField(name = "浏览人数")
    var visitNum: Long = 0,
    @field:ExcelField(name = "当月营业额")
    var turnover: Float = 0f,
    @field:ExcelField(name = "历史营业额")
    var totalTurnover: Double = 0.0,
    @field:ExcelField(name = "开店时间", dateformat = "yyyy-MM-dd HH:mm:ss")
    var addTime: Date? = null,
    @field:ExcelField(name = "备注", ignore = true)
    var remark: String? = null,
) {
    override fun toString(): String =
        "ShopDTO{vip=$vip, shopName='$shopName', branchNum=$branchNum, shopId=$shopId, visitNum=$visitNum, turnover=$turnover, totalTurnover=$totalTurnover, addTime=$addTime, remark='$remark'}"
}
