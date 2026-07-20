package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "DTO para crear o actualizar un cliente")
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Schema(description = "Nombre del cliente", example = "Juan")
    private String nombre;

    @Size(max = 100)
    @Schema(description = "Apellido del cliente", example = "Garcia")
    private String apellido;

    @Size(max = 100)
    @Schema(description = "UUID generado offline (opcional para creaciones online)", example = "123e4567-e89b-12d3-a456-426614174000")
    private String uuidOffline;

    @Size(max = 30)
    @Schema(description = "Telefono de contacto del cliente", example = "+541112345678")
    private String telefono;

    @Email(message = "El email debe tener un formato valido")
    @Size(max = 150)
    @Schema(description = "Email del cliente (debe ser unico)", example = "juan.garcia@ejemplo.com")
    private String email;

    @Size(max = 20)
    @Schema(description = "DNI del cliente (debe ser unico)", example = "30123456")
    private String dni;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 300)
    @Schema(description = "Direccion del domicilio del cliente", example = "Av. Corrientes 1234, CABA")
    private String direccion;

    @Schema(description = "Latitud del domicilio (opcional, se completa via geocoding)",
            example = "-34.603722")
    private BigDecimal latitud;

    @Schema(description = "Longitud del domicilio (opcional, se completa via geocoding)",
            example = "-58.381592")
    private BigDecimal longitud;
}