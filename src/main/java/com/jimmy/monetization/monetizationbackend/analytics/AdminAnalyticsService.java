package com.jimmy.monetization.monetizationbackend.analytics;

import com.jimmy.monetization.monetizationbackend.analytics.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private final AnalyticsRepository repo;

    public AdminAnalyticsService(AnalyticsRepository repo) {
        this.repo = repo;
    }

    public AdminOverviewResponse getOverview() {
        Object[] row = repo.getPlatformSummary();

        long orders = ((Number) row[0]).longValue();
        long paid = ((Number) row[1]).longValue();
        long revenue = ((Number) row[2]).longValue();
        long tenants = repo.countTenants();

        return new AdminOverviewResponse(revenue, orders, paid, tenants);
    }

    public List<TenantRevenueResponse> getTopTenants() {
        return repo.getTopTenants()
                .stream()
                .map(r -> new TenantRevenueResponse(
                        (java.util.UUID) r[0],
                        ((Number) r[2]).longValue(),
                        ((Number) r[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<GlobalTopProductResponse> getTopProducts() {
        return repo.getGlobalTopProducts()
                .stream()
                .map(r -> new GlobalTopProductResponse(
                        (String) r[0],
                        ((Number) r[2]).longValue(),
                        ((Number) r[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    public List<RevenuePoint> getTimeseries() {
        return repo.getPlatformTimeseries()
                .stream()
                .map(r -> new RevenuePoint(
                        r[0].toString(),
                        ((Number) r[1]).longValue()
                ))
                .collect(Collectors.toList());
    }
}