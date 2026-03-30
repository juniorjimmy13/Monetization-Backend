package com.jimmy.monetization.monetizationbackend.analytics.dto;

public class RevenuePoint {
    private String date;
    private long revenue;

    public RevenuePoint(String date, long revenue) {
        this.date = date;
        this.revenue = revenue;
    }

    public String getDate() { return date; }
    public long getRevenue() { return revenue; }
}