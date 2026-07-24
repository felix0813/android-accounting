package com.wzf.accounting.service

import com.wzf.accounting.util.AmountExtractor

object NotificationFilter {

    private val decimalPattern = Regex("\\d+\\.\\d+")
    private val financialKeywords = listOf(
        "元", "¥", "￥", "$", "€", "£",
        "支付", "付款", "收款", "转账", "消费", "扣款", "到账", "入账", "充值",
        "红包", "话费", "余额", "提现", "还款", "账单", "交易", "订单",
        "payment", "pay", "transfer", "balance", "refund", "charge"
    )

    fun isLikelyFinancialNotification(text: String): Boolean {
        if (!AmountExtractor.containsAmount(text)) return false

        val lowerText = text.lowercase()
        if (financialKeywords.any { kw -> lowerText.contains(kw.lowercase()) }) return true

        if (decimalPattern.containsMatchIn(text)) return true

        return false
    }
}
