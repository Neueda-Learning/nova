package com.nova.portfolio.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    // Separate bean: LLM completions need a materially longer read timeout than FX lookups.
    @Bean
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder, AiProperties aiProperties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(aiProperties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(aiProperties.getReadTimeoutMs()))
                .build();
    }
}

