package com.tienda.entregas.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntregaNotFoundExceptionTest {

    @Test
    void constructor_conMensaje() {
        var ex = new EntregaNotFoundException("No encontrada");
        assertEquals("No encontrada", ex.getMessage());
    }

    @Test
    void constructor_conMensajeYCausa() {
        var cause = new RuntimeException("Interno");
        var ex = new EntregaNotFoundException("No encontrada", cause);
        assertEquals("No encontrada", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
