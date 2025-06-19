package com.tienda.entregas.integration;

import com.tienda.entregas.client.UsuarioClient;
import com.tienda.entregas.dto.EntregaRequest;
import com.tienda.entregas.dto.EntregaResponse;
import com.tienda.entregas.dto.UserResponseDTO;
import com.tienda.entregas.service.EntregaService;
import com.tienda.entregas.service.TokenService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = com.tienda.entregas.EntregasApplication.class)
@ActiveProfiles("test")
@DirtiesContext
@Import(EntregaQueryIntegrationTest.MockTokenConfig.class)
public class EntregaQueryIntegrationTest {

    @Autowired
    private EntregaService entregaService;

    @MockBean
    private UsuarioClient usuarioClient;

    @Test
    @Transactional
    public void testListarEntregasPorRepartidorId() {
        Long repartidorId = 10L;
        EntregaRequest request = EntregaRequest.builder()
                .ordenId(1001L)
                .repartidorId(repartidorId)
                .direccionEntrega("Calle 123")
                .build();

        UserResponseDTO mockUser = new UserResponseDTO();
        mockUser.setUsuarioId(repartidorId);
        mockUser.setRol("REPARTIDOR");
        when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(repartidorId), Mockito.anyString())).thenReturn(mockUser);

        entregaService.crearEntrega(request);

        List<EntregaResponse> entregas = entregaService.listarEntregasPorRepartidor(repartidorId);
        assertEquals(1, entregas.size());
        assertEquals(repartidorId, entregas.get(0).getRepartidorId());

        SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken("mock-user", null));
    }

    @Test
    @Transactional
    public void testListarEntregasPorOrdenId() {
        Long repartidorId = 20L;
        Long ordenId = 101L;

        EntregaRequest request = EntregaRequest.builder()
                .ordenId(ordenId)
                .repartidorId(repartidorId)
                .direccionEntrega("Carrera 456")
                .build();

        UserResponseDTO mockUser = new UserResponseDTO();
        mockUser.setUsuarioId(repartidorId);
        mockUser.setRol("REPARTIDOR");
        when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(repartidorId), Mockito.anyString())).thenReturn(mockUser);

        entregaService.crearEntrega(request);

        List<EntregaResponse> entregas = entregaService.listarEntregasPorOrden(ordenId);
        assertEquals(1, entregas.size());
        assertEquals("Asignado", entregas.get(0).getEstado());

        SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken("mock-user", null)
    );
    }

    @Test
    @Transactional
    public void testListarEntregasPorEmailRepartidor() {
        Long repartidorId = 99L;
        String email = "repartidor@tienda.com";

        EntregaRequest request = EntregaRequest.builder()
                .ordenId(202L)
                .repartidorId(repartidorId)
                .direccionEntrega("Av. Principal")
                .build();

        UserResponseDTO mockUser = new UserResponseDTO();
        mockUser.setUsuarioId(repartidorId);
        mockUser.setRol("REPARTIDOR");
        mockUser.setCorreo(email);
        when(usuarioClient.obtenerUsuarioPorId(Mockito.eq(repartidorId), Mockito.anyString())).thenReturn(mockUser);
        when(usuarioClient.obtenerUsuarioPorEmail(Mockito.eq(email), Mockito.anyString())).thenReturn(mockUser);

        entregaService.crearEntrega(request);

        List<EntregaResponse> entregas = entregaService.listarEntregasPorRepartidorEmail(email);
        assertEquals(1, entregas.size());
        assertEquals(202L, entregas.get(0).getOrdenId());
        
        SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken("mock-user", null));
    }

    @TestConfiguration
    static class MockTokenConfig {
        @Bean
        public TokenService tokenService() {
            return () -> "Bearer mocked-test-token";
        }
    }
}
