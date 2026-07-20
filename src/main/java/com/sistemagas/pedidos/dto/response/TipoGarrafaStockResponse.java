package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TipoGarrafaStockResponse {

    private Long id;
    private String codigo;
    private String descripcion;
    private Integer capacidadKg;
    private BigDecimal precio;
    private boolean activo;
}
