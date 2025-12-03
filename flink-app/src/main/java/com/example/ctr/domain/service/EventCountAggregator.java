package com.example.ctr.domain.service;

import com.example.ctr.domain.model.Event;
import com.example.ctr.domain.model.EventCount;
import org.apache.flink.api.common.functions.AggregateFunction;

public class EventCountAggregator implements AggregateFunction<Event, EventCount, EventCount> {

    @Override
    public EventCount createAccumulator() {
        return EventCount.initial();
    }

    @Override
    public EventCount add(Event event, EventCount accumulator) {
        if (isImpressionEvent(event)) {
            accumulator.incrementImpressions();
        } else if ("click".equals(event.getEventType())) {
            accumulator.incrementClicks();
        }
        return accumulator;
    }

    private boolean isImpressionEvent(Event event) {
        String type = event.getEventType();
        return "view".equals(type) || "impression".equals(type);
    }

    @Override
    public EventCount getResult(EventCount accumulator) {
        return accumulator;
    }

    @Override
    public EventCount merge(EventCount a, EventCount b) {
        return EventCount.merge(a, b);
    }
}
