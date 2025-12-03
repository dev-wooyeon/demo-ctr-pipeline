package com.example.ctr.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@Getter
@Setter
public class AppConfig {

    private KafkaProperties kafka;
    private RedisProperties redis;
    private ClickHouseProperties clickhouse;
    private DuckDBProperties duckdb;
    private CtrConfig ctr;
    private FlinkConfig flink;

    @Getter
    @Setter
    public static class CtrConfig {
        private CtrJobProperties job;
    }

    @Getter
    @Setter
    public static class FlinkConfig {
        private int parallelism;
        private CheckpointConfig checkpoint;
        private StateBackendConfig stateBackend;
        private RestartStrategyConfig restartStrategy;

        @Getter
        @Setter
        public static class CheckpointConfig {
            private boolean enabled;
            private long interval;
            private long timeout;
            private long minPause;
            private int maxConcurrent;
            private String mode;
        }

        @Getter
        @Setter
        public static class StateBackendConfig {
            private String type;
            private String checkpointDir;
            private String savepointDir;
        }

        @Getter
        @Setter
        public static class RestartStrategyConfig {
            private String type;
            private int attempts;
            private long delay;
        }
    }

    public static AppConfig load() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

            // Try loading application-local.yml first, then application.yml
            InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application-local.yml");
            if (is == null) {
                is = AppConfig.class.getClassLoader().getResourceAsStream("application.yml");
            }
            if (is == null) {
                throw new RuntimeException("Configuration file not found (application-local.yml or application.yml)");
            }

            return mapper.readValue(is, AppConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}
