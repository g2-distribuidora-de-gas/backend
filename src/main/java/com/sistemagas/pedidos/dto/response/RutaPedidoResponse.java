package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Parada (pedido) dentro de una ruta planificada")
public class RutaPedidoResponse {

    @Schema(description = "ID de la tabla intermedia RutaPedido", example = "10")
    private Long id;

    @Schema(description = "ID del pedido asociado a esta parada", example = "42")
    private Long pedidoId;

    @Schema(description = "Cliente destino de esta parada")
    private ClienteResponse cliente;

    @Schema(description = "Orden de visita dentro de la ruta (1 = primera parada)", example = "1")
    private Integer orden;

    @Schema(description = "Distancia desde la parada anterior en metros", example = "3200")
    private Integer distanciaDesdeAnteriorM;

    @Schema(description = "Duracion estimada desde la parada anterior en segundos", example = "480")
    private Integer duracionDesdeAnteriorS;

    @Schema(description = "Estado de entrega de esta parada", example = "PENDIENTE")
    private EstadoEntrega estadoEntrega;
}