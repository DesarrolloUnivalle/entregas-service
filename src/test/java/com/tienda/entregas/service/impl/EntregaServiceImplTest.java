package com.tienda.entregas.service.impl;

import com.tienda.entregas.client.UsuarioClient;
import com.tienda.entregas.dto.*;
import com.tienda.entregas.exception.EntregaNotFoundException;
import com.tienda.entregas.exception.RolInvalidoException;
import com.tienda.entregas.kafka.KafkaProducer;
import com.tienda.entregas.model.entity.Entrega;
import com.tienda.entregas.model.entity.Entrega.EntregaStatus;
import com.tienda.entregas.repository.EntregaRepository;
import com.tienda.entregas.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EntregaServiceImplTest {

    @InjectMocks
    private EntregaServiceImpl entregaService;

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(tokenService.obtenerToken()).thenReturn("Bearer mock-token");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearEntrega_deberiaRetornarEntregaResponse() {
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(1L)
                .repartidorId(2L)
                .direccionEntrega("Cra 45")
                .build();

        UserResponseDTO repartidor = new UserResponseDTO();
        repartidor.setUsuarioId(2L);
        repartidor.setRol("REPARTIDOR");

        Entrega entregaMock = new Entrega();
        entregaMock.setId(100L);
        entregaMock.setOrdenId(1L);
        entregaMock.setPedidoId(1L);
        entregaMock.setRepartidorId(2L);
        entregaMock.setEstado(EntregaStatus.Asignado);
        entregaMock.setFechaAsignacion(LocalDateTime.now());
        entregaMock.setDireccionEntrega("Cra 45");

        when(usuarioClient.obtenerUsuarioPorId(2L, "Bearer mock-token")).thenReturn(repartidor);
        when(entregaRepository.save(any(Entrega.class))).thenReturn(entregaMock);

        EntregaResponse response = entregaService.crearEntrega(request);

        assertEquals(1L, response.getOrdenId());
        assertEquals("Asignado", response.getEstado());
        verify(kafkaProducer).publicarEventoEntregaAsignada(any(Entrega.class));
    }

    @Test
    void crearEntrega_deberiaLanzarRolInvalidoException() {
        EntregaRequest request = new EntregaRequest(1L, 2L, "Dirección");

        UserResponseDTO repartidor = new UserResponseDTO();
        repartidor.setUsuarioId(2L);
        repartidor.setRol("CLIENTE");

        when(usuarioClient.obtenerUsuarioPorId(2L, "Bearer mock-token")).thenReturn(repartidor);

        assertThrows(RolInvalidoException.class, () -> entregaService.crearEntrega(request));
    }

    @Test
    void actualizarEstadoEntrega_deberiaActualizarYPublicarEvento() {
        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setEstado(EntregaStatus.Asignado);

        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
        when(entregaRepository.save(any())).thenReturn(entrega);

        EntregaResponse response = entregaService.actualizarEstadoEntrega(1L, "ENTREGADO");

        assertEquals("Entregado", response.getEstado());
        verify(kafkaProducer).publicarEventoEntregaCompletada(entrega);
    }

    @Test
    void actualizarEstadoEntrega_estadoInvalidoDebeLanzarExcepcion() {
        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setEstado(EntregaStatus.Asignado);

        when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));

        assertThrows(IllegalArgumentException.class, () -> entregaService.actualizarEstadoEntrega(1L, "INVALIDO"));
    }

    @Test
    void asignarRepartidorAutomatico_deberiaGuardarYPublicar() {
        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(1L);
        user.setRol("REPARTIDOR");

        when(usuarioClient.obtenerUsuarioPorId(1L, "Bearer mock-token")).thenReturn(user);
        when(entregaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        entregaService.asignarRepartidorAutomatico(1L, "Dirección");

        verify(entregaRepository).save(any());
        verify(kafkaProducer).publicarEventoEntregaAsignada(any());
    }

    @Test
    void listarEntregasPorRepartidor_deberiaRetornarLista() {
        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setRepartidorId(1L);
        entrega.setEstado(EntregaStatus.En_camino);

        when(entregaRepository.findByRepartidorId(1L)).thenReturn(List.of(entrega));

        List<EntregaResponse> lista = entregaService.listarEntregasPorRepartidor(1L);

        assertEquals(1, lista.size());
        assertEquals("En camino", lista.get(0).getEstado());
    }

    @Test
    void listarEntregasPorOrden_deberiaRetornarLista() {
        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setOrdenId(10L);
        entrega.setEstado(EntregaStatus.Entregado);

        when(entregaRepository.findByOrdenId(10L)).thenReturn(List.of(entrega));

        List<EntregaResponse> lista = entregaService.listarEntregasPorOrden(10L);

        assertEquals(1, lista.size());
        assertEquals("Entregado", lista.get(0).getEstado());
    }

    @Test
    void listarEntregasPorRepartidorEmail_deberiaBuscarPorEmailYRetornar() {
        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(1L);

        Entrega entrega = new Entrega();
        entrega.setRepartidorId(1L);
        entrega.setEstado(EntregaStatus.Asignado);

        when(usuarioClient.obtenerUsuarioPorEmail("email@test.com", "Bearer mock-token")).thenReturn(user);
        when(entregaRepository.findByRepartidorId(1L)).thenReturn(List.of(entrega));

        List<EntregaResponse> lista = entregaService.listarEntregasPorRepartidorEmail("email@test.com");

        assertEquals(1, lista.size());
        assertEquals("Asignado", lista.get(0).getEstado());
    }

    @Test
    void listarEntregasPorRepartidorEmail_usuarioNoExisteDebeFallar() {
        when(usuarioClient.obtenerUsuarioPorEmail("email@test.com", "Bearer mock-token")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                entregaService.listarEntregasPorRepartidorEmail("email@test.com"));
    }

    @Test
    void actualizarEstadoEntrega_entregaNoExisteDebeFallar() {
        when(entregaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntregaNotFoundException.class, () ->
                entregaService.actualizarEstadoEntrega(999L, "ASIGNADO"));
    }

    @Test
    void actualizarEstadoEntrega_conEstadoInvalido_deberiaLanzarExcepcion() {
    Long entregaId = 1L;
    Entrega entrega = new Entrega();
    entrega.setId(entregaId);
    entrega.setEstado(Entrega.EntregaStatus.Asignado);

    when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> entregaService.actualizarEstadoEntrega(entregaId, "INEXISTENTE"));

    assertEquals("Estado no válido: INEXISTENTE", ex.getMessage());
    }

    @Test
    void listarEntregasPorRepartidorEmail_conUsuarioNulo_deberiaLanzarExcepcion() {
    String email = "desconocido@correo.com";

    when(tokenService.obtenerToken()).thenReturn("Bearer token");
    when(usuarioClient.obtenerUsuarioPorEmail(email, "Bearer token")).thenReturn(null);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> entregaService.listarEntregasPorRepartidorEmail(email));

    assertEquals("No se encontró el repartidor con email: " + email, ex.getMessage());
    }

    @Test
    void asignarRepartidorAutomatico_conRolInvalido_deberiaLanzarRolInvalidoException() {
    Long ordenId = 101L;
    String direccion = "Calle Falsa 123";

    UserResponseDTO usuarioConRolInvalido = new UserResponseDTO();
    usuarioConRolInvalido.setUsuarioId(1L);
    usuarioConRolInvalido.setRol("CLIENTE"); // 👈 rol inválido

    when(tokenService.obtenerToken()).thenReturn("Bearer token");
    when(usuarioClient.obtenerUsuarioPorId(1L, "Bearer token")).thenReturn(usuarioConRolInvalido);

    assertThrows(RolInvalidoException.class,
        () -> entregaService.asignarRepartidorAutomatico(ordenId, direccion));
    }




}

