package com.jimmy.monetization.monetizationbackend.order.dto;

import com.jimmy.monetization.monetizationbackend.order.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public class CreateOrderResponse {
    private UUID orderId;
    private String orderNumber;
    private OrderStatus status;
    private String productSku;
    private String productName;
    private int priceMinor;
    private String currency;
    private Instant createdAt;

    public CreateOrderResponse(UUID orderId, String orderNumber, OrderStatus status,
                               String productSku, String productName, int priceMinor, String currency, Instant createdAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.productSku = productSku;
        this.productName = productName;
        this.priceMinor = priceMinor;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public UUID getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public OrderStatus getStatus() { return status; }
    public String getProductSku() { return productSku; }
    public String getProductName() { return productName; }
    public int getPriceMinor() { return priceMinor; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
