package com.jimmy.monetization.monetizationbackend.payment.mpesa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MpesaOAuthTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") String expiresIn
) {}