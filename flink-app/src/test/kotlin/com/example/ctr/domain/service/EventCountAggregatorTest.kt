package com.example.ctr.domain.service

import com.example.ctr.domain.model.Event
import com.example.ctr.domain.model.EventCount
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EventCountAggregatorTest {

    private val aggregator = EventCountAggregator()

    @Test
    fun `aggregates view and click events`() {
        val accumulator = aggregator.createAccumulator()
        val view = Event(eventType = "view")
        val click = Event(eventType = "click")

        val afterView = aggregator.add(view, accumulator)
        val afterClick = aggregator.add(click, afterView)
        val result = aggregator.getResult(afterClick)

        assertThat(result.impressions).isEqualTo(1)
        assertThat(result.clicks).isEqualTo(1)
    }

    @Test
    fun `merges accumulators`() {
        val a = EventCount(1, 2)
        val b = EventCount(3, 4)

        val merged = aggregator.merge(a, b)

        assertThat(merged.impressions).isEqualTo(4)
        assertThat(merged.clicks).isEqualTo(6)
    }
}
