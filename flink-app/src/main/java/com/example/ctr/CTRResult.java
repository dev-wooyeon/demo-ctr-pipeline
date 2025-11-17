package com.example.ctr;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class CTRResult {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter WINDOW_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+09:00'").withZone(ZONE_SEOUL);
    private static final DateTimeFormatter REDIS_KEY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm").withZone(ZONE_SEOUL);

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("impressions")
    private long impressions;

    @JsonProperty("clicks")
    private long clicks;

    @JsonProperty("ctr")
    private double ctr;

    @JsonProperty("window_start")
    private String windowStart;

    @JsonProperty("window_end")
    private String windowEnd;

    private long windowStartMs;
    private long windowEndMs;

    @JsonProperty("calculated_at")
    private long calculatedAt;

    public CTRResult() {
        // For Flink / Jackson serialization
    }

    private CTRResult(String productId, long impressions, long clicks, long windowStartMs, long windowEndMs) {
        this.productId = productId;
        this.impressions = impressions;
        this.clicks = clicks;
        this.windowStartMs = windowStartMs;
        this.windowEndMs = windowEndMs;
        this.calculatedAt = System.currentTimeMillis();
        this.ctr = calculateCtr(impressions, clicks);
        this.windowStart = formatWindow(windowStartMs);
        this.windowEnd = formatWindow(windowEndMs);
    }

    public static CTRResult fromCounts(ProductEventCounts counts, long windowStartMs, long windowEndMs) {
        return new CTRResult(counts.getProductId(), counts.getImpressions(), counts.getClicks(), windowStartMs, windowEndMs);
    }

    private static double calculateCtr(long impressions, long clicks) {
        return impressions > 0 ? (double) clicks / impressions : 0.0d;
    }

    private static String formatWindow(long timestampMs) {
        return WINDOW_FORMATTER.format(Instant.ofEpochMilli(timestampMs));
    }

    public String getProductId() {
        return productId;
    }

    public long getImpressions() {
        return impressions;
    }

    public long getClicks() {
        return clicks;
    }

    public double getCtr() {
        return ctr;
    }

    public String getWindowStart() {
        return windowStart;
    }

    public String getWindowEnd() {
        return windowEnd;
    }

    public long getWindowStartMs() {
        return windowStartMs;
    }

    public long getWindowEndMs() {
        return windowEndMs;
    }

    public long getCalculatedAt() {
        return calculatedAt;
    }

    public String getRedisKey(long windowStartMs) {
        String windowTimestamp = REDIS_KEY_FORMATTER.format(Instant.ofEpochMilli(windowStartMs));
        return String.format("ctr:%s:%s", productId, windowTimestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CTRResult ctrResult = (CTRResult) o;
        return impressions == ctrResult.impressions &&
                clicks == ctrResult.clicks &&
                Double.compare(ctrResult.ctr, ctr) == 0 &&
                calculatedAt == ctrResult.calculatedAt &&
                Objects.equals(productId, ctrResult.productId) &&
                Objects.equals(windowStart, ctrResult.windowStart) &&
                Objects.equals(windowEnd, ctrResult.windowEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, impressions, clicks, ctr, windowStart, windowEnd, calculatedAt);
    }

    @Override
    public String toString() {
        return "CTRResult{" +
                "productId='" + productId + '\'' +
                ", impressions=" + impressions +
                ", clicks=" + clicks +
                ", ctr=" + String.format("%.4f", ctr) +
                ", windowStart='" + windowStart + '\'' +
                ", windowEnd='" + windowEnd + '\'' +
                ", calculatedAt=" + calculatedAt +
                '}';
    }
}
