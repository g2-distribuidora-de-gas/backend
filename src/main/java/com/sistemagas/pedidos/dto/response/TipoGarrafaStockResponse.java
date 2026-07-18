package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TipoGarrafaStockResponse {

    private Long id;
    private String codigo;
    private String descripcion;
    private Integer capacidadKg;
    private boolean activo;
}
