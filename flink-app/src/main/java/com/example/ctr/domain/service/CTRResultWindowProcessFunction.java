package com.example.ctr.domain.service;

import com.example.ctr.domain.model.CTRResult;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class CTRResultWindowProcessFunction
        extends ProcessWindowFunction<Tuple2<Long, Long>, CTRResult, String, TimeWindow> {

    @Override
    public void process(String productId, Context context, Iterable<Tuple2<Long, Long>> elements,
            Collector<CTRResult> out) {
        Tuple2<Long, Long> counts = elements.iterator().next();
        long impressions = counts.f0;
        long clicks = counts.f1;
        long windowStart = context.window().getStart();
        long windowEnd = context.window().getEnd();

        CTRResult result = CTRResult.calculate(productId, impressions, clicks, windowStart, windowEnd);
        out.collect(result);
    }
}
