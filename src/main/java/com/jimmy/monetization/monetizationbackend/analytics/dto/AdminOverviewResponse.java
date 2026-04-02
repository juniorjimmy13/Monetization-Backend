package com.jimmy.monetization.monetizationbackend.analytics.dto;

public class AdminOverviewResponse {
    private long revenue;
    private long orders;
    private long paidOrders;
    private long tenants;

    public AdminOverviewResponse(long revenue, long orders, long paidOrders, long tenants) {
        this.revenue = revenue;
        this.orders = orders;
        this.paidOrders = paidOrders;
        this.tenants = tenants;
    }

    public long getRevenue() { return revenue; }
    public long getOrders() { return orders; }
    public long getPaidOrders() { return paidOrders; }
    public long getTenants() { return tenants; }
}