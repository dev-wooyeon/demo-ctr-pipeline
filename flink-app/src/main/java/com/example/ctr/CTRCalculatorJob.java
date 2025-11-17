package com.example.ctr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
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

        KafkaSource<Event> impressionSource = createEventSource(IMPRESSION_TOPIC, "ctr-calculator-impressions", objectMapper);
        KafkaSource<Event> clickSource = createEventSource(CLICK_TOPIC, "ctr-calculator-clicks", objectMapper);

        WatermarkStrategy<Event> watermarkStrategy = WatermarkStrategy
                .<Event>forBoundedOutOfOrderness(WATERMARK_ALLOWED_LAG)
                .withTimestampAssigner(new EventTimestampExtractor());

        DataStream<Event> impressionStream = buildEventStream(env, impressionSource, watermarkStrategy, "Impression Source")
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

    public static class EventCountAggregator implements AggregateFunction<Event, ProductEventCounts, ProductEventCounts> {

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

    public static class CTRResultWindowProcessFunction extends ProcessWindowFunction<ProductEventCounts, CTRResult, String, TimeWindow> {
        @Override
        public void process(String key, Context context, Iterable<ProductEventCounts> elements, Collector<CTRResult> out) {
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
