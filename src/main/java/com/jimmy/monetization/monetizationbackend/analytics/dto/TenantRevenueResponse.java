package com.jimmy.monetization.monetizationbackend.analytics.dto;

import java.util.UUID;

public class TenantRevenueResponse {
    private UUID tenantId;
    private long revenue;
    private long orders;

    public TenantRevenueResponse(UUID tenantId, long revenue, long orders) {
        this.tenantId = tenantId;
        this.revenue = revenue;
        this.orders = orders;
    }

    public UUID getTenantId() { return tenantId; }
    public long getRevenue() { return revenue; }
    public long getOrders() { return orders; }
}