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
    fun removesWhitespaceBeforeMatching() {
        assertEquals(88.00, AmountExtractor.extract("支付  人民币   88.00 元")!!, 0.001)
    }

    @Test
    fun returnsNullForOnlyCardOrOrderNumbers() {
        assertNull(AmountExtractor.extract("交易单号 2024071012345，尾号8862"))
    }
}
