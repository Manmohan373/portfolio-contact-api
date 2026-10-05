package com.manmohan.contactapi;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Contact contact, Cors cors, RateLimit rateLimit) {
    public record Contact(String to, String from) {}
    public record Cors(List<String> allowedOrigins) {}
    public record RateLimit(int maxRequests, int windowMinutes) {}
}
