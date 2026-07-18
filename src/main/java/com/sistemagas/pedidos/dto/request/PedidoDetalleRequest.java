package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Linea de detalle de un pedido: garrafa y cantidad solicitada")
public class PedidoDetalleRequest {

    @NotNull(message = "El ID de tipo de garrafa es obligatorio")
    @Schema(description = "ID del tipo de garrafa solicitada", example = "2")
    private Long tipoGarrafaId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Schema(description = "Cantidad de unidades de la garrafa", example = "1")
    private Integer cantidad;
}