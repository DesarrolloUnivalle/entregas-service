package com.tienda.entregas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.env.Environment;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthFilter jwtAuthFilter;
    private FilterChain filterChain;
    private Environment environment;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        environment = mock(Environment.class); // ← mock del nuevo parámetro
        jwtAuthFilter = new JwtAuthFilter(jwtUtil, environment); // ← aquí lo pasas
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_conTokenValido_deberiaEstablecerAutenticacion() throws ServletException, IOException {
        String token = "valid.token";
        String username = "usuario";

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();

        Authentication mockAuth = mock(Authentication.class);
        when(jwtUtil.validarToken(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(jwtUtil.getAuthentication(token)).thenReturn(mockAuth);
        when(mockAuth.getAuthorities()).thenReturn(java.util.List.of());

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_sinToken_noDebeAutenticar() throws Exception {
        var request = new MockHttpServletRequest(); // sin header
        var response = new MockHttpServletResponse();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenMalFormado_noDebeAutenticar() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "BadFormatToken");
        var response = new MockHttpServletResponse();

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenInvalido_noDebeAutenticar() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token");
        var response = new MockHttpServletResponse();

        when(jwtUtil.validarToken("invalid.token")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_usernameEsNull_noDebeAutenticar() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        when(jwtUtil.validarToken("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_autenticacionYaEstablecida_noDebeSobrescribir() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Verificamos que no se sobrescribe la autenticación existente
        verify(jwtUtil, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_exceptionDuranteValidacion_noDebeFallarFiltro() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        when(jwtUtil.validarToken(anyString())).thenThrow(new RuntimeException("Error interno"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotFilter_conPerfilTest_activoDebeRetornarTrue() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, environment);

        var request = new MockHttpServletRequest();
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_conPerfilNoTest_debeRetornarFalse() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, environment);

        var request = new MockHttpServletRequest();
        assertFalse(filter.shouldNotFilter(request));
    }

}
