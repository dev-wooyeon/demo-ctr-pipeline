package com.example.ctr.application;

import com.example.ctr.domain.model.CTRResult;
import com.example.ctr.domain.model.Event;
import com.example.ctr.domain.service.CTRResultWindowProcessFunction;
import com.example.ctr.domain.service.EventCountAggregator;
import com.example.ctr.infrastructure.flink.sink.ClickHouseSinkAdapter;
import com.example.ctr.infrastructure.flink.sink.DuckDBSinkAdapter;
import com.example.ctr.infrastructure.flink.sink.RedisSinkAdapter;
import com.example.ctr.infrastructure.flink.source.KafkaSourceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CtrJobService {

    private final KafkaSourceAdapter kafkaSourceAdapter;
    private final RedisSinkAdapter redisSinkAdapter;
    private final DuckDBSinkAdapter duckDBSinkAdapter;
    private final ClickHouseSinkAdapter clickHouseSinkAdapter;

    @Value("${kafka.topics.impression}")
    private String impressionTopic;

    @Value("${kafka.topics.click}")
    private String clickTopic;

    @Value("${kafka.group-id}")
    private String groupId;

    @Value("${flink.parallelism}")
    private int parallelism;

    public void execute() throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallelism);

        // Sources
        DataStream<Event> impressionStream = env.fromSource(
                kafkaSourceAdapter.createSource(impressionTopic, groupId),
                WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner(
                                (event, timestamp) -> java.sql.Timestamp.valueOf(event.getTimestamp()).getTime()),
                "Impression Source");

        DataStream<Event> clickStream = env.fromSource(
                kafkaSourceAdapter.createSource(clickTopic, groupId),
                WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                        .withTimestampAssigner(
                                (event, timestamp) -> java.sql.Timestamp.valueOf(event.getTimestamp()).getTime()),
                "Click Source");

        // Union & Transformation
        DataStream<CTRResult> ctrResults = impressionStream.union(clickStream)
                .filter(Event::hasProductId)
                .keyBy(Event::getProductId)
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .allowedLateness(Time.seconds(5))
                .aggregate(new EventCountAggregator(), new CTRResultWindowProcessFunction());

        // Sinks
        ctrResults.addSink(redisSinkAdapter.createSink()).name("Redis Sink");
        ctrResults.addSink(duckDBSinkAdapter.createSink()).name("DuckDB Sink").setParallelism(1);
        ctrResults.addSink(clickHouseSinkAdapter.createSink()).name("ClickHouse Sink");

        ctrResults.print();

        env.execute("CTR Calculator Job (Spring Boot)");
    }
}
