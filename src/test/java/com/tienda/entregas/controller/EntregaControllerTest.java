package com.tienda.entregas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.entregas.dto.EntregaRequest;
import com.tienda.entregas.dto.EntregaResponse;
import com.tienda.entregas.security.JwtUtil;
import com.tienda.entregas.service.EntregaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EntregaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntregaService entregaService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setupJwt() {
        String token = "token-valido-repartidor";
        
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "repartidor@correo.com")
                .build();

        var auth = new UsernamePasswordAuthenticationToken(
                jwt, // ← este Jwt se inyectará como @AuthenticationPrincipal
                null,
                List.of(new SimpleGrantedAuthority("ROLE_REPARTIDOR"))
        );

        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        Mockito.when(jwtUtil.validarToken(Mockito.anyString())).thenReturn(true);
        Mockito.when(jwtUtil.extractUsername(Mockito.anyString())).thenReturn("repartidor@correo.com");
        Mockito.when(jwtUtil.getAuthentication(Mockito.anyString())).thenReturn(auth);
    }

    @Test
    void crearEntrega_deberiaRetornar200() throws Exception {
        EntregaRequest request = EntregaRequest.builder()
        .ordenId(1L)
        .repartidorId(2L)
        .direccionEntrega("Calle Falsa 123")
        .build();
        EntregaResponse response = EntregaResponse.builder().build();

        Mockito.when(entregaService.crearEntrega(any())).thenReturn(response);

        mockMvc.perform(post("/api/entregas")
                        .header("Authorization", "Bearer token-valido-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }


    @Test
    void actualizarEstadoEntrega_conRolRepartidor_deberiaRetornar200() throws Exception {
        EntregaResponse response = EntregaResponse.builder().build();

        Mockito.when(entregaService.actualizarEstadoEntrega(1L, "ENTREGADO")).thenReturn(response);

        mockMvc.perform(put("/api/entregas/1/estado")
                        .header("Authorization", "Bearer token-valido-repartidor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"ENTREGADO\"")) // <-- si es un string plano, entre comillas
                .andExpect(status().isOk());
    }

    @Test
    void listarEntregasPorRepartidor_deberiaRetornar200() throws Exception {
        Mockito.when(entregaService.listarEntregasPorRepartidorEmail("repartidor@correo.com"))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/entregas/repartidor")
                        .header("Authorization", "Bearer token-valido-repartidor"))
            .andExpect(status().isOk());
    }


    @Test
    void listarEntregasPorOrden_sinAuth_deberiaRetornar200() throws Exception {
        Mockito.when(entregaService.listarEntregasPorOrden(1L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/entregas/orden/1"))
                .andExpect(status().isOk());
    }

    @Test
    void listarEntregasPorOrden_sinResultados_deberiaRetornarListaVacia() throws Exception {
        Mockito.when(entregaService.listarEntregasPorOrden(123L))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/entregas/orden/123"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

  

}
