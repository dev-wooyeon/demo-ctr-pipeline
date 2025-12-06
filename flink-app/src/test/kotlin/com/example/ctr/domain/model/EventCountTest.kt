package com.example.ctr.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EventCountTest {

    @Test
    fun `initial should return zero count`() {
        val eventCount = EventCount.initial()
        assertThat(eventCount.impressions).isEqualTo(0L)
        assertThat(eventCount.clicks).isEqualTo(0L)
    }

    @Test
    fun `incrementImpressions should increase value`() {
        val eventCount = EventCount.initial()
        eventCount.incrementImpressions()
        eventCount.incrementImpressions()
        assertThat(eventCount.impressions).isEqualTo(2L)
        assertThat(eventCount.clicks).isZero()
    }

    @Test
    fun `incrementClicks should increase value`() {
        val eventCount = EventCount.initial()
        repeat(3) { eventCount.incrementClicks() }
        assertThat(eventCount.clicks).isEqualTo(3L)
        assertThat(eventCount.impressions).isZero()
    }

    @Test
    fun `merge should combine two counts`() {
        val count1 = EventCount(100L, 5L)
        val count2 = EventCount(200L, 10L)

        val merged = EventCount.merge(count1, count2)
        assertThat(merged.impressions).isEqualTo(300L)
        assertThat(merged.clicks).isEqualTo(15L)
    }

    @Test
    fun `copy should create independent copy`() {
        val original = EventCount(100L, 5L)
        val copy = original.copy()
        copy.incrementImpressions()
        copy.incrementClicks()

        assertThat(original.impressions).isEqualTo(100L)
        assertThat(original.clicks).isEqualTo(5L)
        assertThat(copy.impressions).isEqualTo(101L)
        assertThat(copy.clicks).isEqualTo(6L)
    }

    @Test
    fun `mixed operations should work`() {
        val eventCount = EventCount.initial()
        eventCount.incrementImpressions()
        eventCount.incrementImpressions()
        eventCount.incrementClicks()

        assertThat(eventCount.impressions).isEqualTo(2L)
        assertThat(eventCount.clicks).isEqualTo(1L)
    }
}
