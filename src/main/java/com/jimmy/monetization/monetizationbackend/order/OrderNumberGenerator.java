package com.jimmy.monetization.monetizationbackend.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNumberGenerator {
    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter DF = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    public String generate() {
        String date = LocalDate.now().format(DF);
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 6; i++) suffix.append(ALPHANUM.charAt(RNG.nextInt(ALPHANUM.length())));
        return "ORD-" + date + "-" + suffix;
    }
}
