package com.jimmy.monetization.monetizationbackend.analytics;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public Object summary() {
        return service.getSummary();
    }

    @GetMapping("/revenue-timeseries")
    public Object timeseries() {
        return service.getTimeseries();
    }

    @GetMapping("/top-products")
    public Object topProducts() {
        return service.getTopProducts();
    }
}