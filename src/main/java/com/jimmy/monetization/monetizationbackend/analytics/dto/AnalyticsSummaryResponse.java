package com.jimmy.monetization.monetizationbackend.analytics.dto;

public class AnalyticsSummaryResponse {
    private long revenue;
    private long orders;
    private long paidOrders;
    private double conversionRate;

    public AnalyticsSummaryResponse(long revenue, long orders, long paidOrders) {
        this.revenue = revenue;
        this.orders = orders;
        this.paidOrders = paidOrders;
        this.conversionRate = orders == 0 ? 0 : (double) paidOrders / orders;
    }

    public long getRevenue() { return revenue; }
    public long getOrders() { return orders; }
    public long getPaidOrders() { return paidOrders; }
    public double getConversionRate() { return conversionRate; }
}