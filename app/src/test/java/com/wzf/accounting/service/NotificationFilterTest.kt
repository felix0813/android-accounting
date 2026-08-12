package com.wzf.accounting.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFilterTest {
    @Test
    fun keepsFinancialNotificationWithExplicitAmount() {
        assertTrue(
            NotificationFilter.isLikelyFinancialNotification(
                "扣款通知 您账户6385于2026年08月12日发生快捷支付扣款，人民币187.60"
            )
        )
    }

    @Test
    fun filtersPaymentNotificationContainingOnlyAccountNumber() {
        assertFalse(
            NotificationFilter.isLikelyFinancialNotification(
                "支付提醒 您账户6385，本次没有发生扣款"
            )
        )
    }

    @Test
    fun filtersOrderAndCardNumbersWithoutAmount() {
        assertFalse(
            NotificationFilter.isLikelyFinancialNotification(
                "交易提醒 订单号20260812123456，银行卡尾号6385"
            )
        )
    }
}
