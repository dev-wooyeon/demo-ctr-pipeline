package com.example.ctr;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;
import java.util.Objects;

/**
 * Domain representation of events flowing through the CTR pipeline.
 * Mutable setters are kept for Jackson deserialization, but callers should use the helper
 * predicates instead of working with raw strings.
 */
public class Event {

    private static final String TYPE_IMPRESSION = "impression";
    private static final String TYPE_CLICK = "click";

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("session_id")
    private String sessionId;

    public Event() {
        // Default constructor for Jackson
    }

    public Event(String userId, String productId, long timestamp, String eventType, String sessionId) {
        this.userId = userId;
        this.productId = productId;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isImpression() {
        return matchesType(TYPE_IMPRESSION);
    }

    public boolean isClick() {
        return matchesType(TYPE_CLICK);
    }

    public boolean hasProductId() {
        return productId != null && !productId.isEmpty();
    }

    private boolean matchesType(String expectedType) {
        return eventType != null && eventType.toLowerCase(Locale.ROOT).equals(expectedType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return timestamp == event.timestamp &&
                Objects.equals(userId, event.userId) &&
                Objects.equals(productId, event.productId) &&
                Objects.equals(eventType, event.eventType) &&
                Objects.equals(sessionId, event.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, productId, timestamp, eventType, sessionId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "userId='" + userId + '\'' +
                ", productId='" + productId + '\'' +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
