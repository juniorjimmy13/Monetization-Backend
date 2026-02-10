package com.jimmy.monetization.monetizationbackend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawApiKey) {
        return encoder.encode(rawApiKey);
    }

    public boolean matches(String rawApiKey, String storedHash) {
        return encoder.matches(rawApiKey, storedHash);
    }
}
