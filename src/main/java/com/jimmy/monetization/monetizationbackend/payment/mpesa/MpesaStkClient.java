package com.jimmy.monetization.monetizationbackend.payment.mpesa;

import com.jimmy.monetization.monetizationbackend.payment.MpesaProperties;
import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.StkPushRequest;
import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.StkPushResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MpesaStkClient {

    private final RestClient restClient;
    private final MpesaOAuthClient oauthClient;
    private final MpesaProperties props;

    public MpesaStkClient(MpesaProperties props, MpesaOAuthClient oauthClient, RestClient.Builder builder) {
        this.props = props;
        this.oauthClient = oauthClient;
        this.restClient = builder.baseUrl(props.getBaseUrl()).build();
    }

    public StkPushResponse push(StkPushRequest req) {
        String token = oauthClient.getAccessToken();

        return restClient.post()
                .uri("/mpesa/stkpush/v1/processrequest")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(StkPushResponse.class);
    }
}