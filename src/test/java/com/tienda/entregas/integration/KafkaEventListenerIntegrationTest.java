package com.tienda.entregas.integration;

import com.tienda.entregas.client.UsuarioClient;
import com.tienda.entregas.dto.PedidoCreadoEvent;
import com.tienda.entregas.model.entity.Entrega;
import com.tienda.entregas.repository.EntregaRepository;
import com.tienda.entregas.service.TokenService;
import com.tienda.entregas.service.impl.EntregaServiceImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import com.tienda.entregas.dto.UserResponseDTO;

@SpringBootTest(classes = com.tienda.entregas.EntregasApplication.class)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = { "pedido-creado" })
@DirtiesContext
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@Import(KafkaEventListenerIntegrationTest.MockTokenConfig.class)
public class KafkaEventListenerIntegrationTest {

    @Autowired
    private EntregaServiceImpl entregaService;

    @Autowired
    private EntregaRepository entregaRepository;

    @MockBean
    private UsuarioClient usuarioClient;

    @Test
    @Transactional
    public void testProcesamientoDeEventoPedidoCreado() {
        // Arrange
        Long ordenId = 99L;
        String direccion = "Calle falsa 123";

        // Simula el usuario con ID hardcodeado (1L)
        UserResponseDTO usuarioMock = new UserResponseDTO();
        usuarioMock.setUsuarioId(1L);
        usuarioMock.setRol("REPARTIDOR");

        Mockito.when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(1L), Mockito.anyString()))
               .thenReturn(usuarioMock);

        PedidoCreadoEvent event = new PedidoCreadoEvent();
        event.setOrdenId(ordenId);
        event.setDireccionEntrega(direccion);
        event.setFechaCreacion(LocalDateTime.now());

        // Act
        entregaService.asignarRepartidorAutomatico(event.getOrdenId(), event.getDireccionEntrega());

        // Assert
        Optional<Entrega> entregaOptional = entregaRepository.findByOrdenId(ordenId).stream().findFirst();
        Assertions.assertTrue(entregaOptional.isPresent(), "La entrega no fue creada");

        Entrega entrega = entregaOptional.get();
        Assertions.assertEquals(direccion, entrega.getDireccionEntrega());
        Assertions.assertEquals(ordenId, entrega.getOrdenId());
    }

    @TestConfiguration
    static class MockTokenConfig {
        @Bean
        public TokenService tokenService() {
            return () -> "Bearer test-token";
        }
    }
}
