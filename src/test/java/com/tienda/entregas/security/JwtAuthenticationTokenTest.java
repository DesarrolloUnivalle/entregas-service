package com.tienda.entregas.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationTokenTest {

    @Test
    void constructor_deberiaInicializarCorrectamente() {
        String username = "testUser";
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        JwtAuthenticationToken token = new JwtAuthenticationToken(username, authorities);

        assertEquals(username, token.getPrincipal(), "El principal debe coincidir con el proporcionado");
        assertEquals(authorities, token.getAuthorities(), "Las autoridades deben coincidir con las proporcionadas");
        assertTrue(token.isAuthenticated(), "El token debe estar autenticado por defecto");
        assertNull(token.getCredentials(), "Las credenciales deben ser nulas");
    }

    @Test
    void getPrincipal_deberiaRetornarElUsuario() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("admin", List.of());
        assertEquals("admin", token.getPrincipal());
    }

    @Test
    void getAuthorities_deberiaRetornarListaCorrecta() {
        var roles = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_USER")
        );
        JwtAuthenticationToken token = new JwtAuthenticationToken("someone", roles);
        assertEquals(2, token.getAuthorities().size());
        assertTrue(token.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void isAuthenticated_deberiaSerTruePorDefecto() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("usuario", List.of());
        assertTrue(token.isAuthenticated());
    }

    @Test
    void getCredentials_deberiaSerSiempreNull() {
        JwtAuthenticationToken token = new JwtAuthenticationToken("cualquiera", List.of());
        assertNull(token.getCredentials());
    }
}
