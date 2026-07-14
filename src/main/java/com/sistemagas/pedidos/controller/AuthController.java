package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.LoginRequest;
import com.sistemagas.pedidos.dto.request.RegisterRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.AuthResponse;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.service.AuthService;
import com.sistemagas.pedidos.util.AuthenticationHelper;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constantes.API_AUTH)
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login y registro de usuarios")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationHelper authenticationHelper;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Autentica con email y contraseña, devuelve un token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login exitoso"));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Registrar nuevo usuario",
            description = "Crea un nuevo usuario. Solo ADMIN (crea PREVENTISTA/REPARTIDOR) " +
                    "y SUPER_ADMIN (crea PREVENTISTA/REPARTIDOR/ADMIN) pueden usar este endpoint.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        Usuario creador = authenticationHelper.getUsuarioAutenticado();
        AuthResponse response = authService.register(request, creador);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Usuario registrado exitosamente"));
    }
}