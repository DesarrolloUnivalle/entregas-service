package com.tienda.entregas.model.entity;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.lang.reflect.Field;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntregaTest {

    private final Validator validator;

    public EntregaTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void gettersAndSetters_deberianFuncionarCorrectamente() {
        Entrega entrega = new Entrega();
        LocalDateTime now = LocalDateTime.now();

        entrega.setId(1L);
        entrega.setOrdenId(2L);
        entrega.setPedidoId(3L);
        entrega.setRepartidorId(4L);
        entrega.setEstado(Entrega.EntregaStatus.Entregado);
        entrega.setFechaEntrega(now);
        entrega.setUbicacionActual("Carrera 12 #45-89");
        entrega.setDireccionEntrega("Calle 23 #45-67");
        entrega.setFechaAsignacion(now);
        entrega.setFechaInicio(now);
        entrega.setObservaciones("Entregado sin novedades");

        assertEquals(1L, entrega.getId());
        assertEquals(2L, entrega.getOrdenId());
        assertEquals(3L, entrega.getPedidoId());
        assertEquals(4L, entrega.getRepartidorId());
        assertEquals(Entrega.EntregaStatus.Entregado, entrega.getEstado());
        assertEquals(now, entrega.getFechaEntrega());
        assertEquals("Carrera 12 #45-89", entrega.getUbicacionActual());
        assertEquals("Calle 23 #45-67", entrega.getDireccionEntrega());
        assertEquals(now, entrega.getFechaAsignacion());
        assertEquals(now, entrega.getFechaInicio());
        assertEquals("Entregado sin novedades", entrega.getObservaciones());
    }

    @Test
    void validacion_deberiaFallarConCamposNulos() {
        Entrega entrega = new Entrega();
        Set<ConstraintViolation<Entrega>> violaciones = validator.validate(entrega);

        assertFalse(violaciones.isEmpty());
        assertEquals(4, violaciones.size()); // ordenId, pedidoId, repartidorId, direccionEntrega
    }

    @Test
    void enumEntregaStatus_deberiaRetornarCorrectamenteElValor() {
        Entrega.EntregaStatus status = Entrega.EntregaStatus.En_camino;
        assertEquals("En camino", status.getValor());
        assertEquals("En camino", status.toString());
    }

    @Test
    void getEstado_deberiaRetornarNullParaEstadoDesconocido() throws Exception {
        Entrega entrega = new Entrega();

        // Simulamos que el campo estado tiene un valor inválido (no presente en el enum)
        Field estadoField = Entrega.class.getDeclaredField("estado");
        estadoField.setAccessible(true);
        estadoField.set(entrega, "ValorInvalido");

        assertNull(entrega.getEstado());
    }

    @Test
    void getEstado_deberiaRetornarEnumCorrecto() {
        Entrega entrega = new Entrega();
        entrega.setEstado(Entrega.EntregaStatus.Cancelado);
        assertEquals(Entrega.EntregaStatus.Cancelado, entrega.getEstado());
    }
} 
