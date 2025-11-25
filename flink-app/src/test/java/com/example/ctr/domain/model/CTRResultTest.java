package com.example.ctr.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CTRResultTest {

    @Test
    void calculate_ShouldReturnCorrectCTR() {
        // Given
        String productId = "product1";
        long impressions = 100;
        long clicks = 5;
        long windowStart = 1000L;
        long windowEnd = 2000L;

        // When
        CTRResult result = CTRResult.calculate(productId, impressions, clicks, windowStart, windowEnd);

        // Then
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getImpressions()).isEqualTo(impressions);
        assertThat(result.getClicks()).isEqualTo(clicks);
        assertThat(result.getCtr()).isEqualTo(0.05);
        assertThat(result.getWindowStart()).isEqualTo(windowStart);
        assertThat(result.getWindowEnd()).isEqualTo(windowEnd);
    }

    @Test
    void calculate_ShouldReturnZeroCTR_WhenImpressionsIsZero() {
        // Given
        long impressions = 0;
        long clicks = 5;

        // When
        CTRResult result = CTRResult.calculate("p1", impressions, clicks, 0, 0);

        // Then
        assertThat(result.getCtr()).isEqualTo(0.0);
    }
}
