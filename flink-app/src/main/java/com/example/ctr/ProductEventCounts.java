package com.example.ctr;

import java.util.Objects;

public class ProductEventCounts {

    private String productId;
    private long impressions;
    private long clicks;

    private ProductEventCounts(String productId, long impressions, long clicks) {
        this.productId = productId;
        this.impressions = impressions;
        this.clicks = clicks;
    }

    public static ProductEventCounts empty() {
        return new ProductEventCounts(null, 0, 0);
    }

    public static ProductEventCounts of(String productId, long impressions, long clicks) {
        return new ProductEventCounts(productId, impressions, clicks);
    }

    public void registerProduct(String productId) {
        if (this.productId == null && productId != null) {
            this.productId = productId;
        }
    }

    public void addEvent(Event event) {
        registerProduct(event.getProductId());
        if (event.isImpression()) {
            incrementImpression();
        } else if (event.isClick()) {
            incrementClick();
        }
    }

    public void incrementImpression() {
        this.impressions++;
    }

    public void incrementClick() {
        this.clicks++;
    }

    public void merge(ProductEventCounts other) {
        if (other == null) {
            return;
        }
        registerProduct(other.productId);
        this.impressions += other.impressions;
        this.clicks += other.clicks;
    }

    public String getProductId() {
        return productId;
    }

    public long getImpressions() {
        return impressions;
    }

    public long getClicks() {
        return clicks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductEventCounts that = (ProductEventCounts) o;
        return impressions == that.impressions &&
                clicks == that.clicks &&
                Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, impressions, clicks);
    }

    @Override
    public String toString() {
        return "ProductEventCounts{" +
                "productId='" + productId + '\'' +
                ", impressions=" + impressions +
                ", clicks=" + clicks +
                '}';
    }
}
