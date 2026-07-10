package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.UsuarioService;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;

@RestController
@RequestMapping(Constantes.API_USUARIOS)
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "API para la gestión de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Listar todos los usuarios", description = "Retorna la lista completa de usuarios")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarTodos(
            @Parameter(description = "Filtra usuarios actualizados despues de este instante (ISO-8601)",
                    example = "2026-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant minUpdatedAt,
            @Parameter(description = "Cantidad maxima de usuarios a retornar", example = "100")
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        List<UsuarioResponse> usuarios = usuarioService.listarTodos(minUpdatedAt, limit);
        return ResponseEntity.ok(ApiResponse.ok(usuarios));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Crear nuevo usuario", description = "Crea un nuevo usuario en el sistema")
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse response = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Desactivar usuario", description = "Desactiva lógicamente un usuario")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id) {
        Usuario solicitante = getUsuarioAutenticado();
        usuarioService.eliminar(id, solicitante);
        return ResponseEntity.ok(ApiResponse.ok(null, "Usuario desactivado exitosamente"));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reactivar usuario", description = "Vuelve a activar un usuario que fue dado de baja lógica")
    public ResponseEntity<ApiResponse<Void>> reactivar(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id) {
        Usuario solicitante = getUsuarioAutenticado();
        usuarioService.reactivar(id, solicitante);
        return ResponseEntity.ok(ApiResponse.ok(null, "Usuario reactivado exitosamente"));
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en la base de datos"));
    }
}

