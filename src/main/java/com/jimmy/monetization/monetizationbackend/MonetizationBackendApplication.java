package com.jimmy.monetization.monetizationbackend;

import com.jimmy.monetization.monetizationbackend.payment.MpesaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(MpesaProperties.class)
public class MonetizationBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonetizationBackendApplication.class, args);
    }

}
