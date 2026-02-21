package com.jimmy.monetization.monetizationbackend.payment.mpesa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StkPushResponse(
        @JsonProperty("MerchantRequestID") String merchantRequestId,
        @JsonProperty("CheckoutRequestID") String checkoutRequestId,
        @JsonProperty("ResponseCode") String responseCode,
        @JsonProperty("ResponseDescription") String responseDescription,
        @JsonProperty("CustomerMessage") String customerMessage,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("errorMessage") String errorMessage
) {
    public boolean accepted() {
        return "0".equals(responseCode) && checkoutRequestId != null && !checkoutRequestId.isBlank();
    }
}