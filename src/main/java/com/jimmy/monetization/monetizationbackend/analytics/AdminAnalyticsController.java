package com.jimmy.monetization.monetizationbackend.analytics;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService service;

    public AdminAnalyticsController(AdminAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public Object overview() {
        return service.getOverview();
    }

    @GetMapping("/top-tenants")
    public Object tenants() {
        return service.getTopTenants();
    }

    @GetMapping("/top-products")
    public Object products() {
        return service.getTopProducts();
    }

    @GetMapping("/revenue-timeseries")
    public Object timeseries() {
        return service.getTimeseries();
    }
}