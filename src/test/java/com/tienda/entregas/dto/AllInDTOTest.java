package com.tienda.entregas.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AllInDTOTest {

    private static Validator validator;

    private ObjectMapper objectMapper;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
    objectMapper = new ObjectMapper(); // 👈 Instanciación manual
    objectMapper.registerModule(new JavaTimeModule()); // 👈 Habilita LocalDateTime
}


    @Test
    void testEntregaAsignadaEventBuilderAndData() {
        LocalDateTime now = LocalDateTime.now();
        EntregaAsignadaEvent event = EntregaAsignadaEvent.builder()
                .ordenId(1L)
                .repartidorId(2L)
                .estado("ASIGNADA")
                .fechaAsignacion(now)
                .build();

        assertEquals(1L, event.getOrdenId());
        assertEquals(2L, event.getRepartidorId());
        assertEquals("ASIGNADA", event.getEstado());
        assertEquals(now, event.getFechaAsignacion());
    }

    @Test
    void testEntregaCompletadaEvent() {
        LocalDateTime now = LocalDateTime.now();
        EntregaCompletadaEvent event = new EntregaCompletadaEvent();
        event.setEntregaId(1L);
        event.setOrdenId(2L);
        event.setFechaEntrega(now);

        assertEquals(1L, event.getEntregaId());
        assertEquals(2L, event.getOrdenId());
        assertEquals(now, event.getFechaEntrega());
    }

    @Test
    void testEntregaRequestValid() {
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(1L)
                .repartidorId(2L)
                .direccionEntrega("Calle 123")
                .build();

        Set<ConstraintViolation<EntregaRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testEntregaRequestInvalid() {
        EntregaRequest request = new EntregaRequest();
        Set<ConstraintViolation<EntregaRequest>> violations = validator.validate(request);
        assertEquals(3, violations.size());
    }

    @Test
    void testEntregaResponseBuilder() {
        LocalDateTime now = LocalDateTime.now();
        EntregaResponse response = EntregaResponse.builder()
                .id(1L)
                .ordenId(2L)
                .pedidoId(3L)
                .repartidorId(4L)
                .estado("ENTREGADO")
                .fechaAsignacion(now)
                .fechaInicio(now)
                .fechaEntrega(now)
                .direccionEntrega("Carrera 45")
                .build();

        assertEquals(1L, response.getId());
        assertEquals("ENTREGADO", response.getEstado());
        assertEquals("Carrera 45", response.getDireccionEntrega());
    }

    @Test
    void testPedidoCreadoEvent() {
        LocalDateTime now = LocalDateTime.now();
        PedidoCreadoEvent event = new PedidoCreadoEvent();
        event.setOrdenId(99L);
        event.setDireccionEntrega("Diagonal 22");
        event.setFechaCreacion(now);

        assertEquals(99L, event.getOrdenId());
        assertEquals("Diagonal 22", event.getDireccionEntrega());
        assertEquals(now, event.getFechaCreacion());
    }

    @Test
    void testUserResponseDTO() {
        UserResponseDTO user = new UserResponseDTO();
        user.setUsuarioId(10L);
        user.setNombre("Juan");
        user.setCorreo("juan@test.com");
        user.setRol("ADMIN");

        assertEquals(10L, user.getUsuarioId());
        assertEquals("Juan", user.getNombre());
        assertEquals("juan@test.com", user.getCorreo());
        assertEquals("ADMIN", user.getRol());
        assertTrue(user.toString().contains("Juan"));
    }

    @Test
    void serializacion_entregaAsignadaEvent_deberiaSerCorrecta() throws Exception {
    EntregaAsignadaEvent original = EntregaAsignadaEvent.builder()
        .ordenId(10L)
        .repartidorId(5L)
        .estado("ASIGNADO")
        .fechaAsignacion(LocalDateTime.now())
        .build();

    String json = objectMapper.writeValueAsString(original);
    EntregaAsignadaEvent deserialized = objectMapper.readValue(json, EntregaAsignadaEvent.class);

    assertEquals(original.getOrdenId(), deserialized.getOrdenId());
    assertEquals(original.getRepartidorId(), deserialized.getRepartidorId());
    assertEquals(original.getEstado(), deserialized.getEstado());
    assertEquals(original.getFechaAsignacion().withNano(0), deserialized.getFechaAsignacion().withNano(0)); // evitar fallo por nanos
    }

    @Test
    void serializacion_entregaCompletadaEvent_deberiaSerCorrecta() throws Exception {
    EntregaCompletadaEvent original = new EntregaCompletadaEvent();
    original.setEntregaId(1L);
    original.setOrdenId(2L);
    original.setFechaEntrega(LocalDateTime.now());

    String json = objectMapper.writeValueAsString(original);
    EntregaCompletadaEvent deserialized = objectMapper.readValue(json, EntregaCompletadaEvent.class);

    assertEquals(original.getEntregaId(), deserialized.getEntregaId());
    assertEquals(original.getOrdenId(), deserialized.getOrdenId());
    assertEquals(original.getFechaEntrega().withNano(0), deserialized.getFechaEntrega().withNano(0));
    }

    @Test
    void serializacion_entregaResponse_deberiaSerCorrecta() throws Exception {
    EntregaResponse original = EntregaResponse.builder()
        .id(1L)
        .ordenId(2L)
        .pedidoId(3L)
        .repartidorId(4L)
        .estado("ENTREGADO")
        .fechaAsignacion(LocalDateTime.now())
        .fechaInicio(LocalDateTime.now())
        .fechaEntrega(LocalDateTime.now())
        .direccionEntrega("Calle 45 #8-19")
        .build();

    String json = objectMapper.writeValueAsString(original);
    EntregaResponse deserialized = objectMapper.readValue(json, EntregaResponse.class);

    assertEquals(original.getId(), deserialized.getId());
    assertEquals(original.getEstado(), deserialized.getEstado());
    assertEquals(original.getDireccionEntrega(), deserialized.getDireccionEntrega());
    }

    @Test
    void serializacion_pedidoCreadoEvent_deberiaSerCorrecta() throws Exception {
    PedidoCreadoEvent original = new PedidoCreadoEvent();
    original.setOrdenId(99L);
    original.setDireccionEntrega("Cra 9 #20-12");
    original.setFechaCreacion(LocalDateTime.now());

    String json = objectMapper.writeValueAsString(original);
    PedidoCreadoEvent deserialized = objectMapper.readValue(json, PedidoCreadoEvent.class);

    assertEquals(original.getOrdenId(), deserialized.getOrdenId());
    assertEquals(original.getDireccionEntrega(), deserialized.getDireccionEntrega());
    assertEquals(original.getFechaCreacion().withNano(0), deserialized.getFechaCreacion().withNano(0));
    }

} 
