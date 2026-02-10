package com.jimmy.monetization.monetizationbackend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Sha256 {
    private Sha256() {}

    public static String hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
