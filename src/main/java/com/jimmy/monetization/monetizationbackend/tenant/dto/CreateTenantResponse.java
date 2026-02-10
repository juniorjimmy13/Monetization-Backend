package com.jimmy.monetization.monetizationbackend.tenant.dto;

import java.util.UUID;

public class CreateTenantResponse {
    private UUID tenantId;
    private String name;
    private String apiKey; // shown once

    public CreateTenantResponse(UUID tenantId, String name, String apiKey) {
        this.tenantId = tenantId;
        this.name = name;
        this.apiKey = apiKey;
    }

    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getApiKey() { return apiKey; }
}
