package com.tienda.entregas.config;

import com.tienda.entregas.service.TokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MockTokenConfig {

    @Bean
    public TokenService tokenService() {
        return () -> "Bearer mocked-test-token";
    }
}