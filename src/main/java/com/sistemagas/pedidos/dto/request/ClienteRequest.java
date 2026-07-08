package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClienteRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;

    @NotBlank(message = "La direccion es obligatoria")
    private String direccion;

    private BigDecimal latitud;
    
    private BigDecimal longitud;
}
