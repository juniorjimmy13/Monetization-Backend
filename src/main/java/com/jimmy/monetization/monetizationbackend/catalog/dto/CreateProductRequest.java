package com.jimmy.monetization.monetizationbackend.catalog.dto;

public class CreateProductRequest {
    private String sku;
    private String name;
    private String description;
    private int priceMinor;
    private String currency;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPriceMinor() { return priceMinor; }
    public void setPriceMinor(int priceMinor) { this.priceMinor = priceMinor; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
