package com.jimmy.monetization.monetizationbackend.payment.mpesa;

import com.jimmy.monetization.monetizationbackend.payment.MpesaProperties;
import com.jimmy.monetization.monetizationbackend.payment.mpesa.dto.MpesaTokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class MpesaOAuthClient {

    private final RestClient restClient;
    private final MpesaProperties props;

    public MpesaOAuthClient(MpesaProperties props, RestClient.Builder builder) {
        this.props = props;
        this.restClient = builder.baseUrl(props.getBaseUrl()).build();
    }

    public String getAccessToken() {
        String basic = Base64.getEncoder().encodeToString(
                (props.getConsumerKey() + ":" + props.getConsumerSecret()).getBytes(StandardCharsets.UTF_8)
        );

        MpesaTokenResponse res = restClient.get()
                .uri("/oauth/v1/generate?grant_type=client_credentials")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(MpesaTokenResponse.class);

        if (res == null || res.accessToken() == null) throw new IllegalStateException("No access token from Mpesa");
        return res.accessToken();
    }
}