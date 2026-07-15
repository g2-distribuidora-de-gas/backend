package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DetalleResumenResponse {
    private Long pedidoDetalleId;
    private Integer cantidadSolicitada;
    private Integer cantidadEntregada;
}