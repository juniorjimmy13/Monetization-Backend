package com.jimmy.monetization.monetizationbackend.order;

public enum OrderStatus {
    INITIATED,
    PENDING_PAYMENT,
    PAYMENT_CONFIRMED,
    ENTITLEMENT_GRANTED,
    COMPLETED,
    FAILED,
    CANCELLED
}
