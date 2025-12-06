package com.example.ctr.infrastructure.flink.source.deserializer

import com.example.ctr.domain.model.Event
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class EventDeserializationSchema : DeserializationSchema<Event> {

    companion object {
        private val log = LoggerFactory.getLogger(EventDeserializationSchema::class.java)
        private val objectMapper: ObjectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(SimpleModule().addDeserializer(LocalDateTime::class.java, LocalDateTimeFlexibleDeserializer()))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    override fun deserialize(message: ByteArray): Event? {
        return try {
            val event = objectMapper.readValue(message, Event::class.java)
            if (event == null || !event.isValid()) {
                log.warn("Dropping invalid event: {}", event)
                return null
            }
            event
        } catch (ex: Exception) {
            log.warn("Failed to deserialize event payload", ex)
            null
        }
    }

    override fun isEndOfStream(nextElement: Event?): Boolean = false

    override fun getProducedType(): TypeInformation<Event> = TypeInformation.of(Event::class.java)
}
