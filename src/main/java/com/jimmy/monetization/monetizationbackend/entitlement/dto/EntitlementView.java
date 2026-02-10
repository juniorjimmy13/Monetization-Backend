package com.jimmy.monetization.monetizationbackend.entitlement.dto;

import java.time.Instant;
import java.util.UUID;

public record EntitlementView(
        UUID id,
        UUID orderId,
        String status,
        Instant grantedAt,
        Instant expiresAt
) {}
