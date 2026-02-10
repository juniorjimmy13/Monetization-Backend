package com.jimmy.monetization.monetizationbackend.payment.dto;

import com.jimmy.monetization.monetizationbackend.payment.PaymentStatus;

import java.util.UUID;

public class InitiatePaymentResponse {
    private UUID paymentAttemptId;
    private PaymentStatus status;
    private String checkoutRequestId;
    private String merchantRequestId;
    private String message;

    public InitiatePaymentResponse(UUID paymentAttemptId, PaymentStatus status,
                                   String checkoutRequestId, String merchantRequestId, String message) {
        this.paymentAttemptId = paymentAttemptId;
        this.status = status;
        this.checkoutRequestId = checkoutRequestId;
        this.merchantRequestId = merchantRequestId;
        this.message = message;
    }

    public UUID getPaymentAttemptId() { return paymentAttemptId; }
    public PaymentStatus getStatus() { return status; }
    public String getCheckoutRequestId() { return checkoutRequestId; }
    public String getMerchantRequestId() { return merchantRequestId; }
    public String getMessage() { return message; }
}
