package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.RolUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de respuesta para autenticación")
public class AuthResponse {

    @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    @Builder.Default
    private String tipo = "Bearer";

    @Schema(description = "ID del usuario autenticado", example = "1")
    private Long userId;

    @Schema(description = "Email del usuario", example = "admin@sistemagas.com")
    private String email;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String nombreCompleto;

    @Schema(description = "Rol del usuario", example = "ADMIN")
    private RolUsuario rol;
}
