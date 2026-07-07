package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.RolUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para registrar un nuevo usuario en el sistema")
public class RegisterRequest {

    @NotBlank(message = "El nombre no puede estar en blanco")
    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @NotBlank(message = "El apellido no puede estar en blanco")
    @Size(max = 100, message = "El apellido no puede tener más de 100 caracteres")
    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String apellido;

    @NotBlank(message = "El DNI no puede estar en blanco")
    @Size(max = 20, message = "El DNI no puede tener más de 20 caracteres")
    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;

    @Size(max = 30, message = "El teléfono no puede tener más de 30 caracteres")
    @Schema(description = "Teléfono del usuario", example = "+541112345678")
    private String telefono;

    @Size(max = 300, message = "La dirección no puede tener más de 300 caracteres")
    @Schema(description = "Dirección del usuario", example = "Av. Corrientes 1234")
    private String direccion;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 150, message = "El email no puede tener más de 150 caracteres")
    @Schema(description = "Email para login", example = "juan@sistemagas.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    @Schema(description = "Contraseña del usuario", example = "password123")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    @Schema(description = "Rol a asignar al usuario", example = "PREVENTISTA")
    private RolUsuario rol;
}
