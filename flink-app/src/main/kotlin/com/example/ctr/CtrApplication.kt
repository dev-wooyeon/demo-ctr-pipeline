package com.example.ctr

import com.example.ctr.application.CtrJobService
import com.example.ctr.config.AppConfig
import com.example.ctr.domain.service.CTRResultWindowProcessFunction
import com.example.ctr.domain.service.EventCountAggregator
import com.example.ctr.infrastructure.flink.CtrJobPipelineBuilder
import com.example.ctr.infrastructure.flink.FlinkEnvironmentFactory
import com.example.ctr.infrastructure.flink.sink.ClickHouseSink
import com.example.ctr.infrastructure.flink.sink.DuckDBSink
import com.example.ctr.infrastructure.flink.sink.RedisSink
import com.example.ctr.infrastructure.flink.source.KafkaSourceFactory
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object CtrApplication {

    private val log = LoggerFactory.getLogger(CtrApplication::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            log.info("Starting CTR Application...")

            val config = AppConfig.load()
            val kafkaSourceFactory = KafkaSourceFactory(config.kafka)
            val redisSink = RedisSink(config.redis)
            val duckDBSink = DuckDBSink(config.duckdb)
            val clickHouseSink = ClickHouseSink(config.clickhouse)
            val flinkEnvironmentFactory = FlinkEnvironmentFactory(config.ctr.job)
            val aggregator = EventCountAggregator()
            val windowFunction = CTRResultWindowProcessFunction()
            val pipelineBuilder = CtrJobPipelineBuilder(
                kafkaSourceFactory,
                redisSink,
                duckDBSink,
                clickHouseSink,
                aggregator,
                windowFunction,
                config.ctr.job
            )

            val jobService = CtrJobService(flinkEnvironmentFactory, pipelineBuilder)
            jobService.execute()
        } catch (ex: Exception) {
            log.error("Fatal error in application startup", ex)
            exitProcess(1)
        }
    }
}
