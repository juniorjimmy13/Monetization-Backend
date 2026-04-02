package com.jimmy.monetization.monetizationbackend.analytics.dto;

public class GlobalTopProductResponse {
    private String sku;
    private long revenue;
    private long sales;

    public GlobalTopProductResponse(String sku, long revenue, long sales) {
        this.sku = sku;
        this.revenue = revenue;
        this.sales = sales;
    }

    public String getSku() { return sku; }
    public long getRevenue() { return revenue; }
    public long getSales() { return sales; }
}