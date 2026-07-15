package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoPedido;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PedidoResumenResponse {
    private Long pedidoId;
    private String uuidOffline;
    private EstadoPedido estado;
    private ClienteResumenResponse cliente;
    private List<DetalleResumenResponse> detalles;
}