package com.tienda.entregas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.entregas.controller.EntregaController;
import com.tienda.entregas.dto.EntregaRequest;
import com.tienda.entregas.dto.EntregaResponse;
import com.tienda.entregas.service.EntregaService;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EntregaController.class)
@Import(JwtAuthFilterIntegrationTest.TestSecurityConfig.class)
class JwtAuthFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private EntregaService entregaService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private final String TOKEN = "mocked.jwt.token";

    @BeforeEach
    void resetearFiltro() {
        reset(jwtAuthFilter);
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/entregas/orden/**").permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }

        @Bean
        public JwtAuthFilter jwtAuthFilter() {
            return mock(JwtAuthFilter.class);
        }
    }

    @Test
    void crearEntrega_conTokenValidoYRolAdmin_deberiaPermitirAcceso() throws Exception {
        when(jwtUtil.validarToken(TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("admin@example.com");
        when(jwtUtil.getAuthentication(TOKEN)).thenReturn(
                new JwtAuthenticationToken("admin@example.com", Collections.singleton(() -> "ROLE_ADMIN"))
        );

        EntregaRequest request = new EntregaRequest();
        EntregaResponse response = mock(EntregaResponse.class);
        when(entregaService.crearEntrega(any())).thenReturn(response);

        mockMvc.perform(post("/api/entregas")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void crearEntrega_sinToken_deberiaRetornar403() throws Exception {
        doAnswer(invocation -> {
    HttpServletResponse response = invocation.getArgument(1);
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Sin token");
    return null;
    }).when(jwtAuthFilter).doFilter(any(), any(), any());

        mockMvc.perform(post("/api/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarEntregasPorRepartidor_conTokenValidoYRolRepartidor_deberiaPermitirAcceso() throws Exception {
        when(jwtUtil.validarToken(TOKEN)).thenReturn(true);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("repartidor@example.com");
        when(jwtUtil.getAuthentication(TOKEN)).thenReturn(
                new JwtAuthenticationToken("repartidor@example.com", Collections.singleton(() -> "ROLE_REPARTIDOR"))
        );

        when(entregaService.listarEntregasPorRepartidorEmail("repartidor@example.com"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/entregas/repartidor")
                .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void listarEntregasPorOrden_sinToken_deberiaPermitirAcceso() throws Exception {
        when(entregaService.listarEntregasPorOrden(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/entregas/orden/1"))
                .andExpect(status().isOk());
    }

    @Test
    void crearEntrega_conTokenInvalido_deberiaRetornar403() throws Exception {
        doAnswer(invocation -> {
    HttpServletResponse response = invocation.getArgument(1);
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token inválido");
    return null;
    }).when(jwtAuthFilter).doFilter(any(), any(), any());

        when(jwtUtil.validarToken(TOKEN)).thenReturn(false);

        mockMvc.perform(post("/api/entregas")
                .header("Authorization", "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void filtroNoSeEjecuta_siPerfilEsTest() throws Exception {
        EntregaRequest request = new EntregaRequest();
        EntregaResponse response = mock(EntregaResponse.class);
        when(entregaService.crearEntrega(any())).thenReturn(response);

        mockMvc.perform(post("/api/entregas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
