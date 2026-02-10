package com.jimmy.monetization.monetizationbackend.payment;

public interface MpesaGateway {
    MpesaGatewayResult stkPush(String phoneNumber, int amountMinor, String transactionDesc, String accountReference);
}
