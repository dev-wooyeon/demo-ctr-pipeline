package com.example.ctr.infrastructure.flink.source.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Jackson deserializer that accepts either formatted strings (yyyy-MM-dd HH:mm:ss)
 * or epoch milliseconds when binding to {@link LocalDateTime}.
 */
public class LocalDateTimeFlexibleDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = parser.currentToken();
        if (currentToken == JsonToken.VALUE_NUMBER_INT) {
            long epochMillis = parser.getLongValue();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        }
        if (currentToken == JsonToken.VALUE_STRING) {
            String value = parser.getText();
            if (value == null || value.isBlank()) {
                return null;
            }
            return LocalDateTime.parse(value.trim(), FORMATTER);
        }
        return null;
    }
}
