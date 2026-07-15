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
    @DisplayName("Devuelve getUsername() cuando el principal es UserDetails (no el toString)")
    void returnsUsernameWhenPrincipalIsUserDetails() {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        "preventista@empresa.com",
                        "x",
                        List.of(new SimpleGrantedAuthority("ROLE_PREVENTISTA"),
                                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                );
        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).contains("preventista@empresa.com");
        assertThat(auditor.get().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Trunca a 100 chars como defensa ante valores grandes")
    void truncatesLongAuditorToFitColumn() {
        String longEmail = "a".repeat(120) + "@empresa.com";
        var authentication = new UsernamePasswordAuthenticationToken(
                longEmail,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PREVENTISTA"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent();
        assertThat(auditor.get().length()).isLessThanOrEqualTo(100);
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