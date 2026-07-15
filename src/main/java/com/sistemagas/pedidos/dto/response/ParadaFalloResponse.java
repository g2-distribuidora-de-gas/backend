package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ParadaFalloResponse {
    private Long rutaPedidoId;
    private Integer orden;
    private String motivoFallo;
    private Instant updatedAt;
    private PedidoResumenResponse pedido;
}