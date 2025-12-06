package com.example.ctr.infrastructure.flink

import com.example.ctr.config.CtrJobProperties
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import java.util.concurrent.TimeUnit

class FlinkEnvironmentFactory(private val properties: CtrJobProperties) {

    fun create(): StreamExecutionEnvironment {
        val env = StreamExecutionEnvironment.getExecutionEnvironment()
        env.parallelism = properties.parallelism

        if (properties.checkpointEnabled) {
            env.enableCheckpointing(properties.checkpointInterval)
            val config: CheckpointConfig = env.checkpointConfig
            config.checkpointingMode = properties.checkpointingMode
            config.setCheckpointStorage(properties.checkpointStorage)
            config.checkpointTimeout = properties.checkpointTimeout
            config.minPauseBetweenCheckpoints = properties.checkpointMinPause
            config.maxConcurrentCheckpoints = properties.checkpointMaxConcurrent
            config.externalizedCheckpointCleanup = properties.externalizedCheckpointCleanup
        }

        env.restartStrategy = RestartStrategies.fixedDelayRestart(
            properties.restartAttempts,
            Time.of(properties.restartDelayMs, TimeUnit.MILLISECONDS)
        )

        return env
    }
}
