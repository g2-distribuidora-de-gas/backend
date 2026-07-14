package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO para informar la cantidad real entregada de un detalle de pedido")
public class DetalleEntregaRequest {

    @NotNull(message = "El ID del detalle es obligatorio")
    @Schema(description = "ID del pedido_detalle", example = "1")
    private Long pedidoDetalleId;

    @NotNull(message = "La cantidad entregada es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Schema(description = "Cantidad real de garrafas entregadas", example = "1")
    private Integer cantidadEntregada;
}
