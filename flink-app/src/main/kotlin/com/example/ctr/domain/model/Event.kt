package com.example.ctr.domain.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.ZoneOffset

@JsonIgnoreProperties(ignoreUnknown = true)
data class Event(
    @JsonProperty("event_id")
    val eventId: String? = null,

    @JsonProperty("event_type")
    val eventType: String? = null,

    @JsonProperty("product_id")
    val productId: String? = null,

    @JsonProperty("user_id")
    val userId: String? = null,

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val timestamp: LocalDateTime? = null,

    @JsonProperty("session_id")
    val sessionId: String? = null
) {

    fun hasProductId(): Boolean = !productId.isNullOrBlank()

    fun isValid(): Boolean = hasProductId() && timestamp != null && VALID_EVENT_TYPES.contains(eventType)

    fun eventTimeMillisUtc(): Long = timestamp?.atOffset(ZoneOffset.UTC)?.toInstant()?.toEpochMilli() ?: 0L

    companion object {
        private val VALID_EVENT_TYPES: Set<String> = setOf("view", "impression", "click")
    }
}
