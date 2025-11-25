package com.example.ctr.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CTRResult {
    private String productId;
    private long impressions;
    private long clicks;
    private double ctr;
    private long windowStart;
    private long windowEnd;

    public static CTRResult calculate(String productId, long impressions, long clicks, long windowStart,
            long windowEnd) {
        double ctr = (impressions == 0) ? 0.0 : (double) clicks / impressions;
        return CTRResult.builder()
                .productId(productId)
                .impressions(impressions)
                .clicks(clicks)
                .ctr(ctr)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .build();
    }
}
