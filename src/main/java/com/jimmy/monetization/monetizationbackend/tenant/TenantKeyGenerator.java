package com.jimmy.monetization.monetizationbackend.tenant;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TenantKeyGenerator {
    private static final String ALPHANUM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RNG = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder("sk_live_");
        for (int i = 0; i < 32; i++) {
            sb.append(ALPHANUM.charAt(RNG.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
