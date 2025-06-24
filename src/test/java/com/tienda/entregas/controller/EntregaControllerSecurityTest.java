package com.tienda.entregas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.entregas.dto.EntregaRequest;
import com.tienda.entregas.service.EntregaService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest
@ActiveProfiles("test") // Carga el perfil de prueba
@AutoConfigureMockMvc
class EntregaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntregaService entregaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void actualizarEstadoEntrega_sinRolAdecuado_deberiaRetornar403() throws Exception {
        var jwt = Jwt.withTokenValue("token-sin-rol")
                .header("alg", "none")
                .claim("sub", "usuario@correo.com")
                .build();

        var jwtAuth = new JwtAuthenticationToken(jwt, List.of()); // sin roles

        mockMvc.perform(put("/api/entregas/1/estado")
                .principal(jwtAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"ENTREGADO\""))
            .andExpect(status().isForbidden());
    }

    @Test
    void crearEntrega_requestInvalido_deberiaRetornar400() throws Exception {
        EntregaRequest request = new EntregaRequest(); // vacío

        var jwt = Jwt.withTokenValue("token-valido-admin")
        .header("alg", "none")
        .claim("sub", "admin@correo.com")
        .claim("role", "ADMIN")
        .build();

        var auth = new UsernamePasswordAuthenticationToken(jwt, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/api/entregas")
                .header("Authorization", "Bearer token-valido-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
}
