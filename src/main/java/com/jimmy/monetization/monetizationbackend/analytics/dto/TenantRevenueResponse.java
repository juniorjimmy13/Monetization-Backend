package com.jimmy.monetization.monetizationbackend.analytics.dto;

import java.util.UUID;

public class TenantRevenueResponse {
    private String tenantName;
    private long revenue;
    private long orders;

    public TenantRevenueResponse(String tenantName, long revenue, long orders) {
        this.tenantName = tenantName;
        this.revenue = revenue;
        this.orders = orders;
    }

    public String getTenantName() { return tenantName; }
    public long getRevenue() { return revenue; }
    public long getOrders() { return orders; }
}