package com.tienda.entregas.exception;

import feign.FeignException;
import feign.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/entregas/1");
    }

    @Test
    void handleEntregaNotFoundException_deberiaRetornar404() {
        ResponseEntity<Object> response = handler.handleEntregaNotFoundException(
                new EntregaNotFoundException("Entrega no encontrada"), webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertBodyContains(response, "Entrega no encontrada", 404);
    }

    @Test
    void handleRolInvalidoException_deberiaRetornar400() {
        ResponseEntity<Object> response = handler.handleRolInvalidoException(
                new RolInvalidoException("Rol inválido"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBodyContains(response, "Rol inválido", 400);
    }

    @Test
    void handleFeignException_deberiaRetornar500SiStatusInvalido() {
        FeignException ex = FeignException.errorStatus("test",
                Response.builder()
                        .status(500)
                        .request(feign.Request.create(feign.Request.HttpMethod.GET, "url", Map.of(), null, StandardCharsets.UTF_8, null))
                        .build());

        ResponseEntity<Object> response = handler.handleFeignException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertBodyContains(response, "Error al comunicarse con otro servicio", 500);
    }

    @Test
    void handleIllegalArgumentException_deberiaRetornar400() {
        ResponseEntity<Object> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Parámetro inválido"), webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBodyContains(response, "Parámetro inválido", 400);
    }

    @Test
    void handleGlobalException_deberiaRetornar500() {
        ResponseEntity<Object> response = handler.handleGlobalException(
                new RuntimeException("Error inesperado"), webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertBodyContains(response, "Ha ocurrido un error inesperado", 500);
    }

    @Test
    void handleMethodArgumentNotValid_deberiaRetornar400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        var error = mock(org.springframework.validation.FieldError.class);
        when(error.getField()).thenReturn("ordenId");
        when(error.getDefaultMessage()).thenReturn("es obligatorio");

        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(error));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, headers, HttpStatus.BAD_REQUEST, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertBodyContains(response, "ordenId: es obligatorio", 400);
    }

    private void assertBodyContains(ResponseEntity<Object> response, String mensajeEsperado, int statusEsperado) {
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(statusEsperado, body.get("status"));
        assertTrue(((String) body.get("message")).contains(mensajeEsperado));
        assertEquals("/api/entregas/1", body.get("path"));
    }
}
