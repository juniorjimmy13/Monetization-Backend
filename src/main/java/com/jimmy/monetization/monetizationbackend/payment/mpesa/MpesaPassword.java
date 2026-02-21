package com.jimmy.monetization.monetizationbackend.payment.mpesa;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class MpesaPassword {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private MpesaPassword() {}

    public static String timestampNow() {
        // Server timezone should be Africa/Nairobi (+03:00); ZonedDateTime.now() will use system TZ.
        return ZonedDateTime.now().format(TS);
    }
    public static String timestampKe() {
        return ZonedDateTime.now(ZoneId.of("Africa/Nairobi")).format(TS);
    }

    public static String password(String shortCode, String passKey, String timestamp) {
        String raw = shortCode + passKey + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}