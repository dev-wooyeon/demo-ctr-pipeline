package com.example.ctr.infrastructure.flink.sink

import com.example.ctr.config.RedisProperties
import com.example.ctr.domain.model.CTRResult
import com.example.ctr.infrastructure.flink.sink.impl.RedisRichSink
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

class RedisSink(private val redisProperties: RedisProperties) {

    fun createSink(): RichSinkFunction<CTRResult> = RedisRichSink(redisProperties.host, redisProperties.port)
}
