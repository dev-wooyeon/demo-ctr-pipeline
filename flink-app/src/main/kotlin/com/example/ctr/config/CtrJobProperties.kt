package com.example.ctr.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup

class CtrJobProperties {

    @field:NotBlank
    var impressionTopic: String = ""

    @field:NotBlank
    var clickTopic: String = ""

    @field:NotBlank
    var groupId: String = ""

    @field:Positive
    var parallelism: Int = 2

    @field:Positive
    var checkpointInterval: Long = 60_000L

    var checkpointEnabled: Boolean = true

    @field:Positive
    var checkpointTimeout: Long = 600_000L

    @field:Positive
    var checkpointMinPause: Long = 5_000L

    @field:Positive
    var checkpointMaxConcurrent: Int = 1

    var externalizedCheckpointCleanup: ExternalizedCheckpointCleanup = ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION

    @field:NotBlank
    var checkpointStorage: String = "file:///tmp/flink-checkpoints"

    var checkpointingMode: CheckpointingMode = CheckpointingMode.EXACTLY_ONCE

    @field:Positive
    var restartAttempts: Int = 3

    @field:Positive
    var restartDelayMs: Long = 10_000L
}
