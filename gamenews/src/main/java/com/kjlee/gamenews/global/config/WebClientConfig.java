package com.kjlee.gamenews.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }
}
