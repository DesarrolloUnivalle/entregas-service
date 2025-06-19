package com.tienda.entregas.service.impl;

import com.tienda.entregas.client.UsuarioClient;
import com.tienda.entregas.dto.EntregaRequest;
import com.tienda.entregas.dto.UserResponseDTO;
import com.tienda.entregas.exception.EntregaNotFoundException;
import com.tienda.entregas.exception.RolInvalidoException;
import com.tienda.entregas.kafka.KafkaProducer;
import com.tienda.entregas.model.entity.Entrega;
import com.tienda.entregas.model.entity.Entrega.EntregaStatus;
import com.tienda.entregas.repository.EntregaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EntregaServiceImplTest {

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private EntregaServiceImpl entregaService;

    @Mock
    private Jwt jwt;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("mocked-token");
    }

    @Test
    void testCrearEntrega_Success() {
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(1L)
                .repartidorId(2L)
                .direccionEntrega("Calle Falsa 123")
                .build();

        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(2L);
        user.setRol("REPARTIDOR");

        when(usuarioClient.obtenerUsuarioPorId(eq(2L), anyString())).thenReturn(user);

        Entrega entregaMock = new Entrega();
        entregaMock.setId(1L);
        entregaMock.setOrdenId(1L);
        entregaMock.setPedidoId(1L);
        entregaMock.setRepartidorId(2L);
        entregaMock.setDireccionEntrega("Calle Falsa 123");
        entregaMock.setEstado(EntregaStatus.Asignado);
        entregaMock.setFechaAsignacion(LocalDateTime.now());

        when(entregaRepository.save(any(Entrega.class))).thenReturn(entregaMock);

        var response = entregaService.crearEntrega(request);

        assertNotNull(response);
        assertEquals(1L, response.getOrdenId());
        assertEquals("Asignado", response.getEstado());
        verify(kafkaProducer).publicarEventoEntregaAsignada(any(Entrega.class));
    }

    @Test
    void testCrearEntrega_RolInvalido() {
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(1L)
                .repartidorId(2L)
                .direccionEntrega("Calle Falsa 123")
                .build();

        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(2L);
        user.setRol("CLIENTE");

        when(usuarioClient.obtenerUsuarioPorId(eq(2L), anyString())).thenReturn(user);

        assertThrows(RolInvalidoException.class, () -> entregaService.crearEntrega(request));
    }

    @Test
    void testActualizarEstadoEntrega_EntregaNoExiste() {
        when(entregaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntregaNotFoundException.class, () -> entregaService.actualizarEstadoEntrega(99L, "Entregado"));
    }

    @Test
    void testAsignarRepartidorAutomatico_Success() {
        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(1L);
        user.setRol("REPARTIDOR");

        when(usuarioClient.obtenerUsuarioPorId(eq(1L), anyString())).thenReturn(user);

        Entrega entregaMock = new Entrega();
        entregaMock.setId(1L);
        entregaMock.setOrdenId(10L);
        entregaMock.setPedidoId(10L);
        entregaMock.setRepartidorId(1L);
        entregaMock.setDireccionEntrega("Calle X");
        entregaMock.setEstado(EntregaStatus.Asignado);
        entregaMock.setFechaAsignacion(LocalDateTime.now());

        when(entregaRepository.save(any(Entrega.class))).thenReturn(entregaMock);

        assertDoesNotThrow(() -> entregaService.asignarRepartidorAutomatico(10L, "Calle X"));
        verify(entregaRepository).save(any());
        verify(kafkaProducer).publicarEventoEntregaAsignada(any());
    }

    @Test
    void testListarEntregasPorRepartidor() {
        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setOrdenId(1L);
        entrega.setRepartidorId(2L);
        entrega.setEstado(EntregaStatus.Asignado);

        when(entregaRepository.findByRepartidorId(2L)).thenReturn(List.of(entrega));

        var entregas = entregaService.listarEntregasPorRepartidor(2L);
        assertEquals(1, entregas.size());
        assertEquals("Asignado", entregas.get(0).getEstado());
    }

    @Test
    void testListarEntregasPorOrden() {
        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setOrdenId(99L);
        entrega.setEstado(EntregaStatus.En_camino);

        when(entregaRepository.findByOrdenId(99L)).thenReturn(List.of(entrega));

        var entregas = entregaService.listarEntregasPorOrden(99L);
        assertEquals(1, entregas.size());
        assertEquals("En camino", entregas.get(0).getEstado());
    }
}
