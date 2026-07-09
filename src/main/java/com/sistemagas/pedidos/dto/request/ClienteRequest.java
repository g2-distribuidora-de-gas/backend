package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
}
