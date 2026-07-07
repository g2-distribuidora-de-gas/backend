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
@Schema(description = "DTO de respuesta para Usuario")
public class UsuarioResponse {

    @Schema(description = "ID del usuario", example = "1")
    private Long id;

    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String apellido;

    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    private String nombreCompleto;

    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;

    @Schema(description = "Teléfono del usuario", example = "+541112345678")
    private String telefono;

    @Schema(description = "Dirección del usuario", example = "Av. Corrientes 1234")
    private String direccion;

    @Schema(description = "Indica si el usuario está activo", example = "true")
    private Boolean activo;

    @Schema(description = "Rol del usuario", example = "PREVENTISTA")
    private RolUsuario rol;

    @Schema(description = "Email del usuario", example = "juan@sistemagas.com")
    private String email;

    @Schema(description = "Fecha de última modificación")
    private java.time.Instant updatedAt;
}
