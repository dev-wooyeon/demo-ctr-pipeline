package com.example.ctr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CTRCalculatorJob {

    private static final String BOOTSTRAP_SERVERS = "kafka1:29092,kafka2:29093,kafka3:29094";
    private static final String IMPRESSION_TOPIC = "impressions";
    private static final String CLICK_TOPIC = "clicks";
    private static final Time WINDOW_SIZE = Time.seconds(10);
    private static final Time WINDOW_LATENESS = Time.seconds(5);
    private static final Duration WATERMARK_ALLOWED_LAG = Duration.ofSeconds(2);
    private static final int PARALLELISM = 2;

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(PARALLELISM);

        ObjectMapper objectMapper = new ObjectMapper();

        KafkaSource<Event> impressionSource = createEventSource(IMPRESSION_TOPIC, "ctr-calculator-impressions",
                objectMapper);
        KafkaSource<Event> clickSource = createEventSource(CLICK_TOPIC, "ctr-calculator-clicks", objectMapper);

        WatermarkStrategy<Event> watermarkStrategy = WatermarkStrategy
                .<Event>forBoundedOutOfOrderness(WATERMARK_ALLOWED_LAG)
                .withTimestampAssigner(new EventTimestampExtractor());

        DataStream<Event> impressionStream = buildEventStream(env, impressionSource, watermarkStrategy,
                "Impression Source")
                .filter(Event::isImpression);

        DataStream<Event> clickStream = buildEventStream(env, clickSource, watermarkStrategy, "Click Source")
                .filter(Event::isClick);

        DataStream<Event> combinedStream = impressionStream.union(clickStream);

        SingleOutputStreamOperator<CTRResult> ctrResults = combinedStream
                .keyBy(new ProductKeySelector())
                .window(TumblingEventTimeWindows.of(WINDOW_SIZE))
                .allowedLateness(WINDOW_LATENESS)
                .aggregate(new EventCountAggregator(), new CTRResultWindowProcessFunction());

        ctrResults.print("CTR Results");

        ctrResults.addSink(new RedisSink());

        // ClickHouse Sink
        ctrResults.addSink(createClickHouseSink()).name("ClickHouse Sink");

        // DuckDB Sink (Parallelism 1 to avoid file locking issues)
        ctrResults.addSink(new DuckDBSink()).name("DuckDB Sink").setParallelism(1);

        // Latency Metric
        ctrResults.map(new LatencyMetricMapper()).name("Latency Metric");

        env.execute("CTR Calculator Job");
    }

    private static KafkaSource<Event> createEventSource(String topic, String groupId, ObjectMapper objectMapper) {
        return KafkaSource.<Event>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new EventDeserializationSchema(objectMapper))
                .build();
    }

    private static DataStream<Event> buildEventStream(StreamExecutionEnvironment env,
            KafkaSource<Event> source,
            WatermarkStrategy<Event> watermarkStrategy,
            String sourceName) {
        return env
                .fromSource(source, watermarkStrategy, sourceName)
                .name(sourceName)
                .uid(sourceName)
                .filter(Event::hasProductId)
                .name(sourceName + " - With ProductId");
    }

    private static SinkFunction<CTRResult> createClickHouseSink() {
        return JdbcSink.sink(
                "INSERT INTO ctr_results (product_id, ctr, impressions, clicks, window_start, window_end) VALUES (?, ?, ?, ?, ?, ?)",
                (statement, result) -> {
                    statement.setString(1, result.getProductId());
                    statement.setDouble(2, result.getCtr());
                    statement.setLong(3, result.getImpressions());
                    statement.setLong(4, result.getClicks());
                    statement.setLong(5, result.getWindowStartMs());
                    statement.setLong(6, result.getWindowEndMs());
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(1000)
                        .withBatchIntervalMs(200)
                        .withMaxRetries(5)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl("jdbc:clickhouse://clickhouse:8123/default")
                        .withDriverName("com.clickhouse.jdbc.ClickHouseDriver")
                        .build());
    }

    public static class DuckDBSink extends org.apache.flink.streaming.api.functions.sink.RichSinkFunction<CTRResult> {
        private transient java.sql.Connection connection;
        private transient java.sql.PreparedStatement preparedStatement;

        @Override
        public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
            super.open(parameters);
            Class.forName("org.duckdb.DuckDBDriver");
            String url = "jdbc:duckdb:/tmp/ctr.duckdb";
            connection = java.sql.DriverManager.getConnection(url);

            // Create table if not exists
            String createTableSql = "CREATE TABLE IF NOT EXISTS ctr_results (" +
                    "product_id VARCHAR, " +
                    "ctr DOUBLE, " +
                    "impressions BIGINT, " +
                    "clicks BIGINT, " +
                    "window_start BIGINT, " +
                    "window_end BIGINT" +
                    ")";
            try (java.sql.Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSql);
            }

            String insertSql = "INSERT INTO ctr_results VALUES (?, ?, ?, ?, ?, ?)";
            preparedStatement = connection.prepareStatement(insertSql);
        }

        @Override
        public void invoke(CTRResult value, Context context) throws Exception {
            preparedStatement.setString(1, value.getProductId());
            preparedStatement.setDouble(2, value.getCtr());
            preparedStatement.setLong(3, value.getImpressions());
            preparedStatement.setLong(4, value.getClicks());
            preparedStatement.setLong(5, value.getWindowStartMs());
            preparedStatement.setLong(6, value.getWindowEndMs());
            preparedStatement.execute();
        }

        @Override
        public void close() throws Exception {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
            super.close();
        }
    }

    public static class LatencyMetricMapper
            extends org.apache.flink.api.common.functions.RichMapFunction<CTRResult, CTRResult> {
        private transient org.apache.flink.metrics.Histogram latencyHistogram;

        @Override
        public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
            super.open(parameters);
            com.codahale.metrics.Histogram dropwizardHistogram = new com.codahale.metrics.Histogram(
                    new com.codahale.metrics.UniformReservoir());
            this.latencyHistogram = getRuntimeContext().getMetricGroup()
                    .histogram("e2e_latency_ms",
                            new org.apache.flink.dropwizard.metrics.DropwizardHistogramWrapper(dropwizardHistogram));
        }

        @Override
        public CTRResult map(CTRResult value) throws Exception {
            long latency = System.currentTimeMillis() - value.getWindowEndMs();
            latencyHistogram.update(latency);
            return value;
        }
    }

    public static class EventDeserializationSchema implements DeserializationSchema<Event> {
        private final ObjectMapper objectMapper;

        public EventDeserializationSchema(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Event deserialize(byte[] message) throws IOException {
            return objectMapper.readValue(message, Event.class);
        }

        @Override
        public boolean isEndOfStream(Event nextElement) {
            return false;
        }

        @Override
        public TypeInformation<Event> getProducedType() {
            return TypeInformation.of(Event.class);
        }
    }

    public static class ProductKeySelector implements KeySelector<Event, String> {
        @Override
        public String getKey(Event event) {
            return event.getProductId();
        }
    }

    public static class EventCountAggregator
            implements AggregateFunction<Event, ProductEventCounts, ProductEventCounts> {

        @Override
        public ProductEventCounts createAccumulator() {
            return ProductEventCounts.empty();
        }

        @Override
        public ProductEventCounts add(Event event, ProductEventCounts accumulator) {
            accumulator.addEvent(event);
            return accumulator;
        }

        @Override
        public ProductEventCounts getResult(ProductEventCounts accumulator) {
            return accumulator;
        }

        @Override
        public ProductEventCounts merge(ProductEventCounts a, ProductEventCounts b) {
            a.merge(b);
            return a;
        }
    }

    public static class CTRResultWindowProcessFunction
            extends ProcessWindowFunction<ProductEventCounts, CTRResult, String, TimeWindow> {
        @Override
        public void process(String key, Context context, Iterable<ProductEventCounts> elements,
                Collector<CTRResult> out) {
            ProductEventCounts counts = elements.iterator().next();
            CTRResult result = CTRResult.fromCounts(counts, context.window().getStart(), context.window().getEnd());
            out.collect(result);
        }
    }

    public static class RedisSink implements SinkFunction<CTRResult> {
        private static final String LATEST_HASH_KEY = "ctr:latest";
        private static final String PREVIOUS_HASH_KEY = "ctr:previous";
        private static final int HASH_TTL_SECONDS = 3600;

        private transient JedisPool jedisPool;
        private transient ObjectMapper objectMapper;

        @Override
        public void invoke(CTRResult ctrResult, Context context) {
            ensureDependencies();

            try (Jedis jedis = jedisPool.getResource()) {
                String field = ctrResult.getProductId();
                String newJsonValue = objectMapper.writeValueAsString(buildPayload(ctrResult));

                moveLatestToPrevious(jedis, field);

                jedis.hset(LATEST_HASH_KEY, field, newJsonValue);
                jedis.expire(LATEST_HASH_KEY, HASH_TTL_SECONDS);
            } catch (Exception e) {
                System.err.println("Error saving to Redis: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void ensureDependencies() {
            if (jedisPool != null && objectMapper != null) {
                return;
            }
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(1);
            jedisPool = new JedisPool(config, "redis", 6379);
            objectMapper = new ObjectMapper();
        }

        private Map<String, Object> buildPayload(CTRResult ctrResult) {
            Map<String, Object> newJsonData = new HashMap<>();
            newJsonData.put("product_id", ctrResult.getProductId());
            newJsonData.put("ctr", Double.parseDouble(String.format("%.4f", ctrResult.getCtr())));
            newJsonData.put("impressions", ctrResult.getImpressions());
            newJsonData.put("clicks", ctrResult.getClicks());
            newJsonData.put("window_start", ctrResult.getWindowStartMs());
            newJsonData.put("window_end", ctrResult.getWindowEndMs());
            return newJsonData;
        }

        private void moveLatestToPrevious(Jedis jedis, String field) {
            String currentLatestValue = jedis.hget(LATEST_HASH_KEY, field);
            if (currentLatestValue == null) {
                return;
            }
            jedis.hset(PREVIOUS_HASH_KEY, field, currentLatestValue);
            jedis.expire(PREVIOUS_HASH_KEY, HASH_TTL_SECONDS);
        }
    }
}
