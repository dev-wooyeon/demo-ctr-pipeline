package com.example.ctr.domain.service

import com.example.ctr.domain.model.Event
import com.example.ctr.domain.model.EventCount
import org.apache.flink.api.common.functions.AggregateFunction

class EventCountAggregator : AggregateFunction<Event, EventCount, EventCount> {

    override fun createAccumulator(): EventCount = EventCount.initial()

    override fun add(event: Event, accumulator: EventCount): EventCount {
        when {
            isImpressionEvent(event) -> accumulator.incrementImpressions()
            event.eventType == "click" -> accumulator.incrementClicks()
        }
        return accumulator
    }

    private fun isImpressionEvent(event: Event): Boolean {
        val type = event.eventType
        return type == "view" || type == "impression"
    }

    override fun getResult(accumulator: EventCount): EventCount = accumulator

    override fun merge(a: EventCount, b: EventCount): EventCount = EventCount.merge(a, b)
}
