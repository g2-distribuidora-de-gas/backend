package com.sistemagas.pedidos.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("securityAuditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_AUDITOR = "SYSTEM";

    private static final int MAX_AUDITOR_LENGTH = 100;

    @NonNull
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM_AUDITOR);
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return Optional.of(SYSTEM_AUDITOR);
        }
        String name;
        if (principal instanceof UserDetails userDetails) {
            name = userDetails.getUsername();
        } else if (principal instanceof String s) {
            name = s;
        } else {
            name = SYSTEM_AUDITOR;
        }
        if (name == null || name.isBlank()) {
            return Optional.of(SYSTEM_AUDITOR);
        }
        if (name.length() > MAX_AUDITOR_LENGTH) {
            name = name.substring(0, MAX_AUDITOR_LENGTH);
        }
        return Optional.of(name);
    }
}