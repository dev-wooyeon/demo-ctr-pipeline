package com.example.ctr.infrastructure.flink.sink.impl

import com.example.ctr.domain.model.CTRResult
import org.apache.flink.configuration.Configuration
import org.apache.flink.metrics.Counter
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement

class DuckDBRichSink(private val duckdbUrl: String) : RichSinkFunction<CTRResult>() {

    private var connection: Connection? = null
    private var preparedStatement: PreparedStatement? = null
    private var successCounter: Counter? = null
    private var failureCounter: Counter? = null

    override fun open(parameters: Configuration) {
        super.open(parameters)
        Class.forName("org.duckdb.DuckDBDriver")
        connection = DriverManager.getConnection(duckdbUrl)

        val createTableSql = """CREATE TABLE IF NOT EXISTS ctr_results (
            product_id VARCHAR,
            ctr DOUBLE,
            impressions BIGINT,
            clicks BIGINT,
            window_start BIGINT,
            window_end BIGINT
        )""".trimIndent()
        connection?.createStatement().use { it?.execute(createTableSql) }

        val insertSql = "INSERT INTO ctr_results VALUES (?, ?, ?, ?, ?, ?)"
        preparedStatement = connection?.prepareStatement(insertSql)
        successCounter = runtimeContext.metricGroup.counter("duckdb_sink_success_total")
        failureCounter = runtimeContext.metricGroup.counter("duckdb_sink_failure_total")
    }

    override fun invoke(value: CTRResult, context: Context) {
        var attempts = 0
        while (attempts < 2) {
            try {
                attempts++
                val ps = preparedStatement ?: throw IllegalStateException("Prepared statement is not initialized")
                ps.setString(1, value.productId)
                ps.setDouble(2, value.ctr)
                ps.setLong(3, value.impressions)
                ps.setLong(4, value.clicks)
                ps.setLong(5, value.windowStart)
                ps.setLong(6, value.windowEnd)
                ps.execute()
                successCounter?.inc()
                return
            } catch (ex: Exception) {
                failureCounter?.inc()
                if (attempts >= 2) {
                    throw ex
                }
                reopen()
            }
        }
    }

    override fun close() {
        preparedStatement?.close()
        connection?.close()
        super.close()
    }

    private fun reopen() {
        close()
        open(Configuration())
    }
}
