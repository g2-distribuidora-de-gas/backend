package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RutaPedidoResponse {
    private Long id; // ID de la tabla intermedia RutaPedido
    private Long pedidoId;
    private ClienteResponse cliente;
    private Integer orden;
    private Integer distanciaDesdeAnteriorM;
    private Integer duracionDesdeAnteriorS;
    private EstadoEntrega estadoEntrega;
}
