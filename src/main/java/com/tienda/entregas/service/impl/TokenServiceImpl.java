package com.tienda.entregas.service.impl;

import com.tienda.entregas.service.TokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@Profile("!test") // Esta implementación no se carga en el perfil de pruebas
public class TokenServiceImpl implements TokenService {

    @Override
    public String obtenerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No se pudo obtener el token de autenticación desde el SecurityContext");
        }

        return "Bearer " + jwt.getTokenValue();
    }
}
