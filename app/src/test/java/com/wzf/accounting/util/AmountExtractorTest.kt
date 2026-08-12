package com.wzf.accounting.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountExtractorTest {
    @Test
    fun extractsAmountAfterPaymentKeyword() {
        assertEquals(12.50, AmountExtractor.extract("微信支付: 你已成功付款 ¥12.50 给 美团外卖")!!, 0.001)
    }

    @Test
    fun skipsCardTailAndDateWhenAmountHasYuanUnit() {
        assertEquals(256.80, AmountExtractor.extract("您尾号8862的储蓄卡于07月10日消费支出人民币 256.80元")!!, 0.001)
    }

    @Test
    fun extractsExplicitAmountInsteadOfAccountNumber() {
        assertEquals(
            187.60,
            AmountExtractor.extract("您账户6385于xxxxxxxxx发生快捷支付扣款，人民币187.60")!!,
            0.001
        )
    }

    @Test
    fun titleKeywordDoesNotTurnAccountNumberIntoAmount() {
        assertEquals(
            187.60,
            AmountExtractor.extract("扣款通知 您账户6385于2026年08月12日发生快捷支付扣款，人民币187.60")!!,
            0.001
        )
    }

    @Test
    fun skipsAccountNumberWhenAmountHasNoCurrencyUnit() {
        assertEquals(
            187.60,
            AmountExtractor.extract("扣款通知 您账户6385于2026年08月12日发生快捷支付扣款187.60")!!,
            0.001
        )
    }

    @Test
    fun removesWhitespaceBeforeMatching() {
        assertEquals(88.00, AmountExtractor.extract("支付  人民币   88.00 元")!!, 0.001)
    }

    @Test
    fun returnsNullForOnlyCardOrOrderNumbers() {
        assertNull(AmountExtractor.extract("交易单号 2024071012345，尾号8862"))
    }

    @Test
    fun returnsNullWhenPaymentNotificationContainsOnlyAccountNumber() {
        assertNull(AmountExtractor.extract("支付提醒 您账户6385，本次没有发生扣款"))
    }

    @Test
    fun extractsCommaSeparatedAmountWithCurrencySymbol() {
        assertEquals(1234.56, AmountExtractor.extract("快捷支付成功，实付￥1,234.56")!!, 0.001)
    }
}
