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
@Schema(description = "Linea de detalle de un pedido en la respuesta")
public class PedidoDetalleResponse {

    @Schema(description = "ID del detalle", example = "100")
    private Long id;

    @Schema(description = "ID de la garrafa", example = "2")
    private Long garrafaId;

    @Schema(description = "Tipo de garrafa", example = "GARRAFA_10KG")
    private TipoGarrafa garrafaTipo;

    @Schema(description = "Cantidad de unidades solicitadas", example = "1")
    private Integer cantidad;

    @Schema(description = "Precio unitario historico al momento de crear el pedido", example = "15000.00")
    private BigDecimal precioUnitario;

    @Schema(description = "Subtotal de la linea (cantidad * precioUnitario)", example = "15000.00")
    private BigDecimal subtotal;
}