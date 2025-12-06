package com.example.ctr.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CTRResultTest {

    @Test
    fun `calculate should return correct CTR`() {
        val productId = "product1"
        val impressions = 100L
        val clicks = 5L
        val windowStart = 1_000L
        val windowEnd = 2_000L

        val result = CTRResult.calculate(productId, impressions, clicks, windowStart, windowEnd)

        assertThat(result.productId).isEqualTo(productId)
        assertThat(result.impressions).isEqualTo(impressions)
        assertThat(result.clicks).isEqualTo(clicks)
        assertThat(result.ctr).isEqualTo(0.05)
        assertThat(result.windowStart).isEqualTo(windowStart)
        assertThat(result.windowEnd).isEqualTo(windowEnd)
    }

    @Test
    fun `calculate should return zero CTR when impressions are zero`() {
        val result = CTRResult.calculate("p1", 0, 5, 0, 0)
        assertThat(result.ctr).isEqualTo(0.0)
    }
}
