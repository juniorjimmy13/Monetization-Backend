package com.jimmy.monetization.monetizationbackend.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mpesa")
public class MpesaProperties {
    private String baseUrl;
    private String consumerKey;
    private String consumerSecret;

    private String shortCode;
    private String passKey;
    private String callbackUrl;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getConsumerKey() { return consumerKey; }
    public void setConsumerKey(String consumerKey) { this.consumerKey = consumerKey; }

    public String getConsumerSecret() { return consumerSecret; }
    public void setConsumerSecret(String consumerSecret) { this.consumerSecret = consumerSecret; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getPassKey() { return passKey; }
    public void setPassKey(String passKey) { this.passKey = passKey; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}