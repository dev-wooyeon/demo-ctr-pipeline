package com.example.ctr.infrastructure.flink.sink;

import com.example.ctr.domain.model.CTRResult;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClickHouseSinkAdapter {

    @Value("${clickhouse.url}")
    private String url;

    @Value("${clickhouse.driver}")
    private String driverName;

    public SinkFunction<CTRResult> createSink() {
        return JdbcSink.sink(
                "INSERT INTO ctr_results (product_id, ctr, impressions, clicks, window_start, window_end) VALUES (?, ?, ?, ?, ?, ?)",
                (statement, result) -> {
                    statement.setString(1, result.getProductId());
                    statement.setDouble(2, result.getCtr());
                    statement.setLong(3, result.getImpressions());
                    statement.setLong(4, result.getClicks());
                    statement.setLong(5, result.getWindowStart());
                    statement.setLong(6, result.getWindowEnd());
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(1000)
                        .withBatchIntervalMs(200)
                        .withMaxRetries(5)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl(url)
                        .withDriverName(driverName)
                        .build());
    }
}
