package com.jimmy.monetization.monetizationbackend.config;

import com.jimmy.monetization.monetizationbackend.payment.MpesaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MpesaProperties.class)
public class MpesaConfig {}
