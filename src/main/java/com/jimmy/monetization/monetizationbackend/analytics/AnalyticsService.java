package com.jimmy.monetization.monetizationbackend.analytics;

import com.jimmy.monetization.monetizationbackend.analytics.dto.*;
import com.jimmy.monetization.monetizationbackend.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final AnalyticsRepository repo;

    public AnalyticsService(AnalyticsRepository repo) {
        this.repo = repo;
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new IllegalStateException("No tenant");
        return tenantId;
    }

    public AnalyticsSummaryResponse getSummary() {
        UUID tenantId = requireTenant();

        Object[] row = repo.getSummary(tenantId);

        long orders = ((Number) row[0]).longValue();
        long paid = ((Number) row[1]).longValue();
        long revenue = ((Number) row[2]).longValue();

        return new AnalyticsSummaryResponse(revenue, orders, paid);
    }

    public List<RevenuePoint> getTimeseries() {
        UUID tenantId = requireTenant();

        return repo.getRevenueTimeseries(tenantId)
                .stream()
                .map(r -> new RevenuePoint(
                        r[0].toString(),
                        ((Number) r[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<TopProductResponse> getTopProducts() {
        UUID tenantId = requireTenant();

        return repo.getTopProducts(tenantId)
                .stream()
                .map(r -> new TopProductResponse(
                        (String) r[0],                      // sku
                        ((Number) r[2]).longValue(),        // sales
                        ((Number) r[1]).longValue()         // revenue
                ))
                .collect(Collectors.toList());
    }
}