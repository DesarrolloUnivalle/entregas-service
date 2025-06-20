package com.tienda.entregas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.entregas.client.UsuarioClient;
import com.tienda.entregas.dto.EntregaRequest;
import com.tienda.entregas.dto.UserResponseDTO;
import com.tienda.entregas.kafka.KafkaProducer;
import com.tienda.entregas.repository.EntregaRepository;
import com.tienda.entregas.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@SpringBootTest(classes = com.tienda.entregas.EntregasApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({EntregaQueryIntegrationTest.MockTokenConfig.class, com.tienda.entregas.config.MockSecurityConfig.class})
class EntregaQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntregaRepository entregaRepository;

    @MockBean
    private UsuarioClient usuarioClient;

    @MockBean
    private KafkaProducer kafkaProducer;

    @MockBean
    private TokenService tokenService;

@BeforeEach
void setUp() {
    entregaRepository.deleteAll();
    Mockito.when(tokenService.obtenerToken()).thenReturn("Bearer test-token");


    // Simular seguridad
    SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken("repartidor@tienda.com", null, "ROLE_REPARTIDOR")
    );

    // Mock para obtenerUsuarioPorId con cualquier ID
    Mockito.when(usuarioClient.obtenerUsuarioPorId(Mockito.anyLong(), Mockito.anyString()))
           .thenAnswer(invocation -> {
               Long id = invocation.getArgument(0);
               UserResponseDTO user = new UserResponseDTO();
               user.setUsuarioId(id);
               user.setCorreo("repartidor@tienda.com");
               user.setNombre("Mock Repartidor");
               user.setRol("REPARTIDOR");
               return user;
           });

    // Mock para obtenerUsuarioPorEmail
    Mockito.when(usuarioClient.obtenerUsuarioPorEmail(Mockito.anyString(), Mockito.anyString()))
           .thenAnswer(invocation -> {
               String email = invocation.getArgument(0);
               UserResponseDTO user = new UserResponseDTO();
               user.setUsuarioId(1L);
               user.setCorreo(email);
               user.setNombre("Mock Repartidor");
               user.setRol("REPARTIDOR");
               return user;
           });

    // Mock de KafkaProducer para evitar conexión real
    Mockito.doNothing().when(kafkaProducer).publicarEventoEntregaAsignada(Mockito.any());
}


    @Test
    void testListarEntregasPorEmailRepartidor() throws Exception {
        Long repartidorId = 1L;
        String email = "repartidor@tienda.com";

        EntregaRequest request = EntregaRequest.builder()
                .ordenId(500L)
                .repartidorId(repartidorId)
                .direccionEntrega("Av Siempre Viva 123")
                .build();

        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(repartidorId);
        user.setCorreo(email);
        user.setRol("REPARTIDOR");

        Mockito.when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(repartidorId), Mockito.anyString()))
                .thenReturn(user);
        Mockito.when(usuarioClient.obtenerUsuarioPorEmail(Mockito.eq(email), Mockito.anyString()))
                .thenReturn(user);

        // Crear entrega primero
        mockMvc.perform(post("/api/entregas")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated()); // <-- esto espera 201

        // Consultar entregas por email
        mockMvc.perform(get("/api/entregas/repartidor")
                        .with(req -> {
                            req.setRemoteUser(email);
                            return req;
                        })
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].ordenId").value(500L));
    }

    @Test
    void testListarEntregasPorOrdenId() throws Exception {
        Long ordenId = 777L;
        Long repartidorId = 2L;

        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(repartidorId);
        user.setCorreo("repartidor@tienda.com");
        user.setRol("REPARTIDOR");

        Mockito.when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(repartidorId), Mockito.anyString()))
                .thenReturn(user);

        EntregaRequest request = EntregaRequest.builder()
                .ordenId(ordenId)
                .repartidorId(repartidorId)
                .direccionEntrega("Carrera 10 #5-50")
                .build();

        mockMvc.perform(post("/api/entregas")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/entregas/orden/{ordenId}", ordenId)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ordenId").value(ordenId));
    }

    @TestConfiguration
    static class MockTokenConfig {
        @Bean
        public TokenService tokenService() {
            return () -> "Bearer test-token";
        }
    }
}
