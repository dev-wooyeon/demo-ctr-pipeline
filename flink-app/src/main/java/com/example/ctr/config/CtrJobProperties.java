package com.example.ctr.config;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

/**
 * Holds configuration properties for the CTR Flink job.
 */
@Getter
@Setter
public class CtrJobProperties {

    // Kafka topics and consumer group
    @NotBlank
    private String impressionTopic;
    @NotBlank
    private String clickTopic;
    @NotBlank
    private String groupId;
    // Execution
    @Positive
    private int parallelism = 2;
    // Checkpointing
    @Positive
    private long checkpointInterval = 60000L;
    private boolean checkpointEnabled = true;
    @Positive
    private long checkpointTimeout = 600_000L;
    @Positive
    private long checkpointMinPause = 5_000L;
    @Positive
    private int checkpointMaxConcurrent = 1;
    private ExternalizedCheckpointCleanup externalizedCheckpointCleanup = ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION;
    @NotBlank
    private String checkpointStorage = "file:///tmp/flink-checkpoints";
    private CheckpointingMode checkpointingMode = CheckpointingMode.EXACTLY_ONCE;
    // Restart strategy
    @Positive
    private int restartAttempts = 3;
    @Positive
    private long restartDelayMs = 10_000L;

}
