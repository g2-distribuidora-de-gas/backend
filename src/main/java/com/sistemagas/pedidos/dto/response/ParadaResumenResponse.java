package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resumen de la parada vinculada a este pedido")
public class ParadaResumenResponse {

    @Schema(description = "ID de la tabla intermedia RutaPedido", example = "10")
    private Long rutaPedidoId;

    @Schema(description = "Orden de visita dentro de la ruta (1 = primera parada)", example = "3")
    private Integer orden;

    @Schema(description = "Distancia desde la parada anterior en metros", example = "3200")
    private Integer distanciaDesdeAnteriorM;

    @Schema(description = "Duracion estimada desde la parada anterior en segundos", example = "480")
    private Integer duracionDesdeAnteriorS;

    @Schema(description = "Hora estimada de llegada a esta parada", example = "2026-01-15T14:30:00-03:00")
    private OffsetDateTime horaEstimadaLlegada;

    @Schema(description = "Estado actual de la entrega", example = "PENDIENTE")
    private EstadoEntrega estadoEntrega;

    @Schema(description = "Motivo del fallo (solo si estadoEntrega = FALLIDO)",
            example = "Cliente ausente")
    private String motivoFallo;
}
