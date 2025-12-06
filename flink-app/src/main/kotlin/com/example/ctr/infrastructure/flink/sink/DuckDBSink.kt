package com.example.ctr.infrastructure.flink.sink

import com.example.ctr.config.DuckDBProperties
import com.example.ctr.domain.model.CTRResult
import com.example.ctr.infrastructure.flink.sink.impl.DuckDBRichSink
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

class DuckDBSink(private val duckDBProperties: DuckDBProperties) {

    fun createSink(): RichSinkFunction<CTRResult> = DuckDBRichSink(duckDBProperties.url)
}
