package com.example.ctr.infrastructure.flink.source.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LocalDateTimeFlexibleDeserializer : JsonDeserializer<LocalDateTime?>() {

    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): LocalDateTime? {
        return when (parser.currentToken) {
            JsonToken.VALUE_NUMBER_INT -> {
                val epochMillis = parser.longValue
                LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)
            }
            JsonToken.VALUE_STRING -> {
                val value = parser.text
                if (value.isNullOrBlank()) {
                    null
                } else {
                    LocalDateTime.parse(value.trim(), FORMATTER)
                }
            }
            else -> null
        }
    }
}
