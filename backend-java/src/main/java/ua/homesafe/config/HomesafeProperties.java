package ua.homesafe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "homesafe")
public record HomesafeProperties(List<String> corsOrigins, int sessionDays) {
}
