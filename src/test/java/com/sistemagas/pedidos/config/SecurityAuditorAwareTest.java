package com.sistemagas.pedidos.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Devuelve SYSTEM cuando no hay autenticacion en el contexto")
    void returnsSystemWhenUnauthenticated() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).contains("SYSTEM");
    }

    @Test
    @DisplayName("Devuelve el principal cuando hay un usuario autenticado")
    void returnsPrincipalWhenAuthenticated() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "usuario@empresa.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PREVENTISTA"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).contains("usuario@empresa.com");
    }

    @Test
    @DisplayName("Devuelve SYSTEM cuando el principal es anonymousUser")
    void returnsSystemForAnonymous() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "anonymousUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).contains("SYSTEM");
    }
}