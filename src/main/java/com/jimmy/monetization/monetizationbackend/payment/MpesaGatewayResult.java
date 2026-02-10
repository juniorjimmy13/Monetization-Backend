package com.jimmy.monetization.monetizationbackend.payment;

public record MpesaGatewayResult(
        String merchantRequestId,
        String checkoutRequestId,
        String responseCode,
        String responseDescription
) {}
