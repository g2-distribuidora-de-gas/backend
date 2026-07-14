package com.sistemagas.pedidos.util;

import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private static final String CODIGO_USUARIO_NO_ENCONTRADO = "USUARIO_NO_ENCONTRADO";

    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene el Usuario completo desde la base de datos a partir del SecurityContext.
     * Lanza BusinessException con HTTP 404 si el usuario autenticado no existe en BD.
     */
    public Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("No hay contexto de seguridad activo",
                    HttpStatus.UNAUTHORIZED, "NO_AUTHENTICATED");
        }
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "Usuario autenticado no encontrado en la base de datos",
                        HttpStatus.NOT_FOUND,
                        CODIGO_USUARIO_NO_ENCONTRADO));
    }
}