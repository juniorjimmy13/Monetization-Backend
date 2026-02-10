package com.jimmy.monetization.monetizationbackend.entitlement.dto;

import java.util.List;

public record ListEntitlementsResponse(
        String userId,
        List<EntitlementView> owned
) {}
