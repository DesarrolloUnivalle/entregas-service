package com.tienda.entregas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Base64;
import java.util.Date;
import java.util.function.Function;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private final String SECRET_KEY = Base64.getEncoder().encodeToString("clave-secreta-para-test-de-mas-de-32-bytes!!".getBytes());
    private final String ISSUER = "mi-app";
    private final long EXPIRATION = 3600000; // 1 hora

    private String generateTestToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION);
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));

        return Jwts.builder()
            .subject(username)
            .issuer(ISSUER)
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("role", role)
            .signWith(key, SignatureAlgorithm.HS256) // ✅ Esta es la forma moderna
            .compact();
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET_KEY);
        ReflectionTestUtils.setField(jwtUtil, "issuer", ISSUER);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }

    @Test
    void validarToken_deberiaRetornarTrue_paraTokenValido() {
        String token = generateTestToken("usuario1", "ADMIN");
        assertTrue(jwtUtil.validarToken(token));
    }

    @Test
    void validarToken_deberiaRetornarFalse_paraTokenInvalido() {
        String token = "token.invalido";
        assertFalse(jwtUtil.validarToken(token));
    }

    @Test
    void extractUsername_deberiaRetornarElSubject() {
        String token = generateTestToken("usuario123", "USER");
        assertEquals("usuario123", jwtUtil.extractUsername(token));
    }

    @Test
    void extractExpiration_deberiaRetornarFechaDeExpiracion() {
        String token = generateTestToken("usuario123", "USER");
        Date expiration = jwtUtil.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void getAuthentication_deberiaRetornarAutenticacionConRol() {
        String token = generateTestToken("usuario456", "REPARTIDOR");
        Authentication auth = jwtUtil.getAuthentication(token);

        assertEquals("usuario456", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_REPARTIDOR")));
    }

    @Test
    void getAuthentication_deberiaRetornarRolPorDefectoCuandoNoHayClaim() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));

        String token = Jwts.builder()
                .subject("usuarioSinRol")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Authentication auth = jwtUtil.getAuthentication(token);

        assertEquals("usuarioSinRol", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void validarToken_deberiaRetornarFalse_paraTokenConFirmaModificada() {
        String token = generateTestToken("usuarioX", "USER") + "manipulado";
        assertFalse(jwtUtil.validarToken(token));
    }

    @Test
    void getAuthentication_deberiaMantenerPrefijoRoleSiYaLoTiene() {
        String token = generateTestToken("admin", "ROLE_ADMIN");
        Authentication auth = jwtUtil.getAuthentication(token);

        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void extractClaim_deberiaRetornarIssuer() {
        String token = generateTestToken("usuario123", "USER");
        String extractedIssuer = ReflectionTestUtils.invokeMethod(jwtUtil, "extractClaim", token, (Function<Claims, String>) Claims::getIssuer);

        assertEquals(ISSUER, extractedIssuer);
    }

    @Test
    void extractUsername_deberiaLanzarExcepcion_conTokenInvalido() {
        String token = "token.invalido";
        assertThrows(Exception.class, () -> jwtUtil.extractUsername(token));
    }

}
