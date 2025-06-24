package com.tienda.entregas.client;

import com.tienda.entregas.dto.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioClientTest {

    private UsuarioClient usuarioClient;

    private UsuarioClient mockInternalClient;

    @BeforeEach
    void setUp() {
        // Creamos un mock de la interfaz base
        mockInternalClient = mock(UsuarioClient.class);

        // Creamos una implementación anónima que llama a los métodos default pero se apoya en el mock
        usuarioClient = new UsuarioClient() {
            @Override
            public UserResponseDTO obtenerUsuarioPorIdInternal(Long id, String token) {
                return mockInternalClient.obtenerUsuarioPorIdInternal(id, token);
            }

            @Override
            public UserResponseDTO obtenerUsuarioPorEmailInternal(String email, String token) {
                return mockInternalClient.obtenerUsuarioPorEmailInternal(email, token);
            }
        };
    }

    @Test
    void obtenerUsuarioPorId_deberiaRetornarUsuario() {
        // Arrange
        UserResponseDTO esperado = new UserResponseDTO();
        esperado.setUsuarioId(1L);
        esperado.setNombre("Carlos");
        esperado.setCorreo("carlos@correo.com");
        esperado.setRol("ADMIN");

        when(mockInternalClient.obtenerUsuarioPorIdInternal(1L, "Bearer token"))
                .thenReturn(esperado);

        // Act
        UserResponseDTO resultado = usuarioClient.obtenerUsuarioPorId(1L, "Bearer token");

        // Assert
        assertNotNull(resultado);
        assertEquals("Carlos", resultado.getNombre());
        verify(mockInternalClient).obtenerUsuarioPorIdInternal(1L, "Bearer token");
    }

    @Test
    void obtenerUsuarioPorId_conError_deberiaLanzarExcepcion() {
        when(mockInternalClient.obtenerUsuarioPorIdInternal(99L, "Bearer token"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                usuarioClient.obtenerUsuarioPorId(99L, "Bearer token"));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(mockInternalClient).obtenerUsuarioPorIdInternal(99L, "Bearer token");
    }

    @Test
    void obtenerUsuarioPorEmail_deberiaRetornarUsuario() {
        UserResponseDTO esperado = new UserResponseDTO();
        esperado.setUsuarioId(2L);
        esperado.setNombre("Laura");
        esperado.setCorreo("laura@correo.com");
        esperado.setRol("REPARTIDOR");

        when(mockInternalClient.obtenerUsuarioPorEmailInternal("laura@correo.com", "Bearer token"))
                .thenReturn(esperado);

        UserResponseDTO resultado = usuarioClient.obtenerUsuarioPorEmail("laura@correo.com", "Bearer token");

        assertNotNull(resultado);
        assertEquals("Laura", resultado.getNombre());
        verify(mockInternalClient).obtenerUsuarioPorEmailInternal("laura@correo.com", "Bearer token");
    }

    @Test
    void obtenerUsuarioPorEmail_conError_deberiaLanzarExcepcion() {
        when(mockInternalClient.obtenerUsuarioPorEmailInternal("falso@correo.com", "Bearer token"))
                .thenThrow(new RuntimeException("Email no registrado"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                usuarioClient.obtenerUsuarioPorEmail("falso@correo.com", "Bearer token"));

        assertEquals("Email no registrado", ex.getMessage());
        verify(mockInternalClient).obtenerUsuarioPorEmailInternal("falso@correo.com", "Bearer token");
    }
}
