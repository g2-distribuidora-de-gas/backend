package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO para actualizar una parada en lote")
public class SincronizacionParadaItemRequest extends ActualizarParadaRequest {

    @NotNull(message = "El ID de la parada es obligatorio")
    @Schema(description = "ID de la parada (RutaPedido)", example = "10")
    private Long rutaPedidoId;

    @NotNull(message = "El uuidOffline es obligatorio para identificar este evento")
    @Schema(description = "UUID generado offline para identificar este evento", example = "evt-123")
    private String uuidOffline;
}
