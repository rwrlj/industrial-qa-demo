package com.demo.industrial.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tongyi")
@Data
public class TongYiConfig {
    private String apiKey;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}