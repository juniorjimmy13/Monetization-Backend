package com.jimmy.monetization.monetizationbackend.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "mpesa.mock", havingValue = "true", matchIfMissing = true)
public class MockMpesaGateway implements MpesaGateway {

    @Override
    public MpesaGatewayResult stkPush(String phoneNumber, int amountMinor, String transactionDesc, String accountReference) {
        // Fake IDs like Daraja returns
        String merchant = "MR-" + UUID.randomUUID();
        String checkout = "ws_CO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new MpesaGatewayResult(merchant, checkout, "0", "Mock Success");
    }
}
