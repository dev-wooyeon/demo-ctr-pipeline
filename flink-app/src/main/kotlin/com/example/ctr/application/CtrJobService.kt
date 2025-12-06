package com.example.ctr.application

import com.example.ctr.infrastructure.flink.CtrJobPipelineBuilder
import com.example.ctr.infrastructure.flink.FlinkEnvironmentFactory
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.slf4j.LoggerFactory

class CtrJobService(
    private val envFactory: FlinkEnvironmentFactory,
    private val pipelineBuilder: CtrJobPipelineBuilder
) {

    private val log = LoggerFactory.getLogger(CtrJobService::class.java)
    @Volatile
    private var env: StreamExecutionEnvironment? = null

    fun execute() {
        try {
            log.info("Starting CTR Calculator Flink Job (Exactly-once)...")

            val executionEnv = envFactory.create()
            env = executionEnv
            pipelineBuilder.build(executionEnv)
            registerShutdownHook()

            log.info("Executing Flink job...")
            executionEnv.execute("CTR Calculator Job (Exactly-once)")
            log.info("Flink job completed successfully")
        } catch (e: Exception) {
            log.error("Failed to execute Flink job", e)
            throw RuntimeException("Flink job execution failed", e)
        }
    }

    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread({
            log.info("Shutdown hook triggered. Gracefully stopping Flink job...")
            try {
                env?.let {
                    log.info("Flink job shutdown initiated")
                }
            } catch (e: Exception) {
                log.error("Error during graceful shutdown", e)
            }
        }, "flink-shutdown-hook"))
    }
}
