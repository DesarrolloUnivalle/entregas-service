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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @BeforeEach
    void setup() {
        entregaRepository.deleteAll();
        SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken("test-user", null)
        );
    }

    @Test
    void crearEntrega_DeberiaCrearYRetornar201() throws Exception {
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(100L)
                .repartidorId(1L)
                .direccionEntrega("Calle 123")
                .build();

        UserResponseDTO usuarioMock = new UserResponseDTO();
        usuarioMock.setUsuarioId(1L);
        usuarioMock.setNombre("Repartidor");
        usuarioMock.setCorreo("repartidor@example.com");
        usuarioMock.setRol("REPARTIDOR");

        Mockito.when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(1L), Mockito.anyString()))
               .thenReturn(usuarioMock);

        String requestJson = new ObjectMapper().writeValueAsString(request);

        mockMvc.perform(MockMvcRequestBuilders.post("/entregas")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
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
