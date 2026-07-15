package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClienteResumenResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
}