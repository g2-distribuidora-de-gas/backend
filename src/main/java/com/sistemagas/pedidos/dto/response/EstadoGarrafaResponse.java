package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstadoGarrafaResponse {

    private Long id;
    private String codigo;
    private String descripcion;
    private boolean activo;
}
