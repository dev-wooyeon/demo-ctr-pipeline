package com.example.ctr.domain.model

/**
 * Counts impressions (view events) and clicks for a specific product.
 */
data class EventCount(
    var impressions: Long = 0L,
    var clicks: Long = 0L
) {

    fun incrementImpressions() {
        impressions++
    }

    fun incrementClicks() {
        clicks++
    }

    companion object {
        fun initial(): EventCount = EventCount()

        fun merge(a: EventCount, b: EventCount): EventCount =
            EventCount(a.impressions + b.impressions, a.clicks + b.clicks)
    }
}
