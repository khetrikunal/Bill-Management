package com.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProps(String jwtSecret, long jwtExpiryMinutes, String corsOrigins,
                       String adminUsername, String adminPassword) {
}
