package com.example.ctr.infrastructure.flink.source.deserializer;

import com.example.ctr.domain.model.Event;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;

public class EventDeserializationSchema implements DeserializationSchema<Event> {

    private static final Logger log = LoggerFactory.getLogger(EventDeserializationSchema.class);
    private static final ObjectMapper objectMapper;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeFlexibleDeserializer());
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(module)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Event deserialize(byte[] message) throws IOException {
        try {
            Event event = objectMapper.readValue(message, Event.class);
            if (event == null || !event.isValid()) {
                log.warn("Dropping invalid event: {}", event);
                return null;
            }
            return event;
        } catch (Exception e) {
            log.warn("Failed to deserialize event payload", e);
            return null;
        }
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
