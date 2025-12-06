package com.example.ctr.domain.model

/**
 * CTR aggregation result for a particular product window.
 */
data class CTRResult(
    val productId: String,
    val impressions: Long,
    val clicks: Long,
    val ctr: Double,
    val windowStart: Long,
    val windowEnd: Long
) {

    companion object {
        fun calculate(productId: String, impressions: Long, clicks: Long, windowStart: Long, windowEnd: Long): CTRResult {
            val ctrValue = if (impressions == 0L) 0.0 else clicks.toDouble() / impressions
            return CTRResult(
                productId = productId,
                impressions = impressions,
                clicks = clicks,
                ctr = ctrValue,
                windowStart = windowStart,
                windowEnd = windowEnd
            )
        }
    }
}
