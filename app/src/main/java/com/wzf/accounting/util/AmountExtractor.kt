package com.wzf.accounting.util

object AmountExtractor {
    private val whitespacePattern = Regex("\\s+")

    private val amountNumber = "(\\d{1,9}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d{1,9}(?:\\.\\d{1,2})?)"

    private val keywordBeforeAmountPatterns = listOf(
        Regex("(?:支付|付款|成功付款|消费支出|消费|扣款|支出|转账|充值|红包|金额|人民币|合计|实付|收款|到账|入账|还款)[^\\d￥¥$]{0,12}[￥¥$]?$amountNumber(?:元)?"),
        Regex("(?:payment|pay|paid|charge|transfer|amount|total)[^\\d￥¥$]{0,12}[￥¥$]?$amountNumber(?:yuan)?", RegexOption.IGNORE_CASE)
    )

    private val amountWithUnitPatterns = listOf(
        Regex("[￥¥$]$amountNumber"),
        Regex("$amountNumber(?:元|圆|块|yuan)", RegexOption.IGNORE_CASE)
    )

    fun extract(text: String): Double? {
        val normalized = text.replace(whitespacePattern, "")
        return findAmount(normalized, keywordBeforeAmountPatterns)
            ?: findAmount(normalized, amountWithUnitPatterns)
    }

    fun containsAmount(text: String): Boolean = extract(text) != null

    private fun findAmount(text: String, patterns: List<Regex>): Double? {
        patterns.forEach { pattern ->
            pattern.find(text)?.groups?.get(1)?.value?.parseAmount()?.let { return it }
        }
        return null
    }

    private fun String.parseAmount(): Double? =
        replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }
}
