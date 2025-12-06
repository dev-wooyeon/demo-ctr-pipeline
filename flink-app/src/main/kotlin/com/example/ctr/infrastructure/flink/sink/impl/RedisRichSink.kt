package com.example.ctr.infrastructure.flink.sink.impl

import com.example.ctr.domain.model.CTRResult
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.configuration.Configuration
import org.apache.flink.metrics.Counter
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPooled

class RedisRichSink(private val redisHost: String, private val redisPort: Int) : RichSinkFunction<CTRResult>() {

    private var jedis: JedisPooled? = null
    private var objectMapper: ObjectMapper? = null
    private var successCounter: Counter? = null
    private var failureCounter: Counter? = null

    override fun open(parameters: Configuration) {
        super.open(parameters)
        val config = DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(2_000)
            .socketTimeoutMillis(2_000)
            .build()
        jedis = JedisPooled(HostAndPort(redisHost, redisPort), config)
        objectMapper = ObjectMapper()
        successCounter = runtimeContext.metricGroup.counter("redis_sink_success_total")
        failureCounter = runtimeContext.metricGroup.counter("redis_sink_failure_total")
    }

    override fun invoke(value: CTRResult, context: Context) {
        var attempts = 0
        while (attempts < 3) {
            try {
                attempts++
                val json = objectMapper?.writeValueAsString(value) ?: ""
                jedis?.hset("ctr:latest", value.productId, json)
                successCounter?.inc()
                return
            } catch (ex: Exception) {
                failureCounter?.inc()
                if (attempts >= 3) {
                    throw ex
                }
                Thread.sleep(50L * attempts)
            }
        }
    }

    override fun close() {
        super.close()
    }
}
