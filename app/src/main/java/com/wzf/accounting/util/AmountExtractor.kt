package com.wzf.accounting.util

object AmountExtractor {
    private val whitespacePattern = Regex("\\s+")

    private val amountNumber = "(?<![\\d,])\\d{1,9}(?:,\\d{3})*(?:\\.\\d{1,2})?(?![\\d,])"

    private val explicitAmountPatterns = listOf(
        Regex("(?:人民币|RMB|CNY|￥|¥|\\$)\\s*($amountNumber)", RegexOption.IGNORE_CASE),
        Regex("($amountNumber)\\s*(?:元|圆|块|yuan|RMB|CNY)", RegexOption.IGNORE_CASE)
    )

    private val keywordBeforeAmountPattern = Regex(
        "(?:支付|付款|成功付款|消费支出|消费|扣款|支出|转账|充值|红包|金额|合计|实付|收款|到账|入账|还款|payment|pay|paid|charge|transfer|amount|total)" +
            "([^\\d￥¥$]{0,12})[￥¥$]?($amountNumber)",
        RegexOption.IGNORE_CASE
    )

    private val identifierKeywords = listOf(
        "账户", "账号", "卡号", "尾号", "银行卡", "订单", "单号", "交易号", "流水号",
        "验证码", "日期", "时间", "account", "card", "order", "reference"
    )

    fun extract(text: String): Double? {
        val normalized = text.replace(whitespacePattern, " ").trim()

        // Currency symbols and units are the strongest signal and must win over an
        // earlier card number or date that merely follows a keyword in the title.
        return findAmount(normalized, explicitAmountPatterns)
            ?: findAmountAfterKeyword(normalized)
    }

    fun containsAmount(text: String): Boolean = extract(text) != null

    private fun findAmount(text: String, patterns: List<Regex>): Double? {
        patterns.forEach { pattern ->
            pattern.find(text)?.groups?.get(1)?.value?.parseAmount()?.let { return it }
        }
        return null
    }

    private fun findAmountAfterKeyword(text: String): Double? {
        for (match in keywordBeforeAmountPattern.findAll(text)) {
            val contextBetweenKeywordAndNumber = match.groups[1]?.value.orEmpty()
            if (identifierKeywords.any { keyword ->
                    contextBetweenKeywordAndNumber.contains(keyword, ignoreCase = true)
                }
            ) {
                continue
            }

            match.groups[2]?.value?.parseAmount()?.let { return it }
        }
        return null
    }

    private fun String.parseAmount(): Double? =
        replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }
}
