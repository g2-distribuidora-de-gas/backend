package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Linea de detalle del pedido en la vista del repartidor")
public class DeliveryDetalleResponse {

    @Schema(description = "ID del detalle", example = "100")
    private Long id;

    @Schema(description = "ID de la garrafa", example = "2")
    private Long garrafaId;

    @Schema(description = "Tipo de garrafa", example = "GARRAFA_10KG")
    private TipoGarrafa garrafaTipo;

    @Schema(description = "Cantidad solicitada en el pedido", example = "2")
    private Integer cantidad;

    @Schema(description = "Cantidad efectivamente entregada (null si la parada aun esta PENDIENTE)",
            example = "2")
    private Integer cantidadEntregada;

    @Schema(description = "Precio unitario al momento de crear el pedido", example = "5500.00")
    private BigDecimal precioUnitario;

    @Schema(description = "Subtotal (cantidadEntregada ?? cantidad) * precioUnitario", example = "5500.00")
    private BigDecimal subtotal;
}
