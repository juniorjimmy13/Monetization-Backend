package com.jimmy.monetization.monetizationbackend.payment.mpesa;

import com.jimmy.monetization.monetizationbackend.payment.MpesaGateway;
import com.jimmy.monetization.monetizationbackend.payment.MpesaGatewayResult;
import com.jimmy.monetization.monetizationbackend.payment.MpesaProperties;
import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.StkPushRequest;
import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.StkPushResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Primary
@Component
public class DarajaMpesaGateway implements MpesaGateway {

    private final RestClient rest;
    private final MpesaProperties props;
    private final MpesaOAuthClient oauth;

    public DarajaMpesaGateway(RestClient.Builder builder, MpesaProperties props, MpesaOAuthClient oauth) {
        this.rest = builder.baseUrl(props.getBaseUrl()).build();
        this.props = props;
        this.oauth = oauth;
    }

    @Override
    public MpesaGatewayResult stkPush(String phone254, int amountMinor, String desc, String accountRef) {
        // Daraja expects normal shillings amounts (not cents). If your system is “minor units”,
        // and KES has no decimals, amountMinor == amount.
        int amount = amountMinor;

        String token = oauth.getAccessToken();
        String ts = MpesaPassword.timestampKe();
        String pw = MpesaPassword.password(props.getShortCode(), props.getPassKey(), ts);

        StkPushRequest req = new StkPushRequest(
                props.getShortCode(),
                pw,
                ts,
                "CustomerPayBillOnline",
                amount,
                phone254,
                props.getShortCode(),
                phone254,
                props.getCallbackUrl(),
                accountRef,
                desc
        );

        StkPushResponse res = rest.post()
                .uri("/mpesa/stkpush/v1/processrequest")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .body(req)
                .retrieve()
                .body(StkPushResponse.class);

        if (res == null) throw new IllegalStateException("Empty response from Daraja STK push");

        // Daraja error shape
        if (res.errorMessage() != null && !res.errorMessage().isBlank()) {
            throw new IllegalStateException("Daraja STK error: " + res.errorMessage() + " (" + res.errorCode() + ")");
        }

        return new MpesaGatewayResult(
                res.merchantRequestId(),
                res.checkoutRequestId(),
                res.responseCode(),
                res.responseDescription()
        );
    }
}