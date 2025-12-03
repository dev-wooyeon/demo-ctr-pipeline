package com.example.ctr;

import com.example.ctr.application.CtrJobService;
import com.example.ctr.config.AppConfig;
import com.example.ctr.domain.service.CTRResultWindowProcessFunction;
import com.example.ctr.domain.service.EventCountAggregator;
import com.example.ctr.infrastructure.flink.CtrJobPipelineBuilder;
import com.example.ctr.infrastructure.flink.FlinkEnvironmentFactory;
import com.example.ctr.infrastructure.flink.sink.ClickHouseSink;
import com.example.ctr.infrastructure.flink.sink.DuckDBSink;
import com.example.ctr.infrastructure.flink.sink.RedisSink;
import com.example.ctr.infrastructure.flink.source.KafkaSourceFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CtrApplication {

    public static void main(String[] args) {
        try {
            log.info("Starting CTR Application...");

            // 1. Load Configuration
            AppConfig config = AppConfig.load();

            // 2. Initialize Infrastructure Components
            KafkaSourceFactory kafkaSourceFactory = new KafkaSourceFactory(config.getKafka());
            RedisSink redisSink = new RedisSink(config.getRedis());
            DuckDBSink duckDBSink = new DuckDBSink(config.getDuckdb());
            ClickHouseSink clickHouseSink = new ClickHouseSink(config.getClickhouse());
            FlinkEnvironmentFactory flinkEnvironmentFactory = new FlinkEnvironmentFactory(config.getCtr().getJob());

            // 3. Initialize Domain Services
            EventCountAggregator aggregator = new EventCountAggregator();
            CTRResultWindowProcessFunction windowFunction = new CTRResultWindowProcessFunction();

            // 4. Initialize Pipeline Builder
            CtrJobPipelineBuilder pipelineBuilder = new CtrJobPipelineBuilder(
                    kafkaSourceFactory,
                    redisSink,
                    duckDBSink,
                    clickHouseSink,
                    aggregator,
                    windowFunction,
                    config.getCtr().getJob());

            // 5. Initialize and Run Job Service
            CtrJobService jobService = new CtrJobService(
                    flinkEnvironmentFactory,
                    pipelineBuilder);

            jobService.execute();

        } catch (Exception e) {
            log.error("Fatal error in application startup", e);
            System.exit(1);
        }
    }
}
