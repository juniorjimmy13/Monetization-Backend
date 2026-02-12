package com.jimmy.monetization.monetizationbackend.entitlement.dto;

import com.jimmy.monetization.monetizationbackend.entitlement.EntitlementStatus;

import java.time.Instant;
import java.util.UUID;

public record EntitlementView(
        String sku,
        EntitlementStatus status,
        Instant grantedAt,
        Instant expiresAt,
        UUID orderId
) {}
