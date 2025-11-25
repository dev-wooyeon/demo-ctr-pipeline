package com.example.ctr.domain.service;

import com.example.ctr.domain.model.Event;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;

public class EventCountAggregator implements AggregateFunction<Event, Tuple2<Long, Long>, Tuple2<Long, Long>> {

    @Override
    public Tuple2<Long, Long> createAccumulator() {
        return Tuple2.of(0L, 0L); // impressions, clicks
    }

    @Override
    public Tuple2<Long, Long> add(Event event, Tuple2<Long, Long> accumulator) {
        if ("view".equals(event.getEventType())) {
            accumulator.f0 += 1;
        } else if ("click".equals(event.getEventType())) {
            accumulator.f1 += 1;
        }
        return accumulator;
    }

    @Override
    public Tuple2<Long, Long> getResult(Tuple2<Long, Long> accumulator) {
        return accumulator;
    }

    @Override
    public Tuple2<Long, Long> merge(Tuple2<Long, Long> a, Tuple2<Long, Long> b) {
        return Tuple2.of(a.f0 + b.f0, a.f1 + b.f1);
    }
}
