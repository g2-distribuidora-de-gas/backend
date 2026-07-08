package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClienteRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 30)
    private String telefono;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 300)
    private String direccion;

    private BigDecimal latitud;
    
    private BigDecimal longitud;
}
