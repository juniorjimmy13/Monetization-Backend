package com.jimmy.monetization.monetizationbackend.tenant.dto;

public class CreateTenantRequest {
    private String name;
    private String webhookUrl;
    private String webhookSecret;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
