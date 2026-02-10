package com.jimmy.monetization.monetizationbackend.payment.dto;

import java.util.UUID;

public class InitiatePaymentRequest {
    private UUID orderId;
    private String phoneNumber;

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
