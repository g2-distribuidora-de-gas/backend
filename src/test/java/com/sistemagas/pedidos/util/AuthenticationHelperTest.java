package com.sistemagas.pedidos.util;

import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationHelperTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthenticationHelper authenticationHelper;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Devuelve el Usuario cuando esta autenticado y existe en BD")
    void returnsUserWhenAuthenticated() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .email("test@test.com")
                .nombre("Test")
                .apellido("User")
                .build();
        var auth = new UsernamePasswordAuthenticationToken(
                "test@test.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));

        Usuario result = authenticationHelper.getUsuarioAutenticado();

        assertThat(result).isEqualTo(usuario);
    }

    @Test
    @DisplayName("Lanza BusinessException con 401 cuando no hay contexto de seguridad")
    void throwsWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authenticationHelper.getUsuarioAutenticado())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No hay contexto de seguridad");
    }

    @Test
    @DisplayName("Lanza BusinessException con 404 cuando el usuario no existe en BD")
    void throwsWhenUserNotInDb() {
        var auth = new UsernamePasswordAuthenticationToken(
                "fantasma@test.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        BusinessException ex = (BusinessException) org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> authenticationHelper.getUsuarioAutenticado()
        );

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getCodigo()).isEqualTo("USUARIO_NO_ENCONTRADO");
    }
}