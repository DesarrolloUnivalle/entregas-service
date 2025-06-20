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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(classes = com.tienda.entregas.EntregasApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({EntregaIntegrationTest.MockTokenConfig.class, com.tienda.entregas.config.MockSecurityConfig.class})
class EntregaIntegrationTest {

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
    void crearEntrega_DeberiaCrearYRetornar201() throws Exception {
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(100L)
                .repartidorId(1L)
                .direccionEntrega("Calle 123")
                .build();

        when(tokenService.obtenerToken()).thenReturn("Bearer mocked-test-token");

        UserResponseDTO usuarioMock = new UserResponseDTO();
        usuarioMock.setUsuarioId(1L);
        usuarioMock.setNombre("Repartidor");
        usuarioMock.setCorreo("repartidor@example.com");
        usuarioMock.setRol("REPARTIDOR");

        Mockito.when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(1L), Mockito.anyString()))
                .thenReturn(usuarioMock);

        String requestJson = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/entregas") // CORREGIDO
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordenId").value(100L))
                .andExpect(jsonPath("$.repartidorId").value(1L));

        Mockito.verify(kafkaProducer).publicarEventoEntregaAsignada(Mockito.any());
    }

    @TestConfiguration
    static class MockTokenConfig {
        @Bean
        public TokenService tokenService() {
            return () -> "Bearer mocked-test-token";
        }
    }
}
