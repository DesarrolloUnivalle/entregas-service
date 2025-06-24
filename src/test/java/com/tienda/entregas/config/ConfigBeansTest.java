package com.tienda.entregas.config;

import com.tienda.entregas.security.JwtAuthFilter;

import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigBeansTest {

    @Test
    void feignLoggerLevel_deberiaSerFULL() {
        FeignClientConfig config = new FeignClientConfig();
        assertEquals(feign.Logger.Level.FULL, config.feignLoggerLevel());
    }

    @Test
    void errorDecoder_deberiaDevolverFeignErrorDecoder() {
        FeignClientConfig config = new FeignClientConfig();
        ErrorDecoder decoder = config.errorDecoder();
        assertNotNull(decoder);
        assertTrue(decoder instanceof FeignClientConfig.FeignErrorDecoder);
    }

    @Test
    void feignErrorDecoder_deberiaDelegarYLoguear() {
        FeignClientConfig.FeignErrorDecoder decoder = new FeignClientConfig.FeignErrorDecoder();

        Response response404 = Response.builder()
                .status(404)
                .request(Request.create(Request.HttpMethod.GET, "http://localhost", Collections.emptyMap(), null, null, null))
                .headers(Collections.emptyMap())
                .build();

        Response response500 = Response.builder()
                .status(500)
                .request(Request.create(Request.HttpMethod.GET, "http://localhost", Collections.emptyMap(), null, null, null))
                .headers(Collections.emptyMap())
                .build();

        assertNotNull(decoder.decode("metodo404", response404));
        assertNotNull(decoder.decode("metodo500", response500));
    }

    @Test
    void securityFilterChain_deberiaConstruirseCorrectamente() throws Exception {
        JwtAuthFilter mockFilter = mock(JwtAuthFilter.class);
        SecurityConfig config = new SecurityConfig(mockFilter);
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        assertNotNull(config.securityFilterChain(http));
    }

    @Test
    void webConfig_noLanzaErrores_alAgregarCorsMappings() {
        WebConfig webConfig = new WebConfig();
        CorsRegistry corsRegistry = new CorsRegistry();
        assertDoesNotThrow(() -> webConfig.addCorsMappings(corsRegistry));
    }
}
