package com.tienda.entregas.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolInvalidoExceptionTest {

    @Test
    void constructor_conMensaje() {
        var ex = new RolInvalidoException("Rol incorrecto");
        assertEquals("Rol incorrecto", ex.getMessage());
    }

    @Test
    void constructor_conMensajeYCausa() {
        var cause = new RuntimeException("Fallo");
        var ex = new RolInvalidoException("Rol incorrecto", cause);
        assertEquals("Rol incorrecto", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
