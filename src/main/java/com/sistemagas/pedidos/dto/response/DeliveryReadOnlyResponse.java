package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vista del pedido optimizada para la app del repartidor")
public class DeliveryReadOnlyResponse {

    @Schema(description = "ID interno del pedido", example = "42")
    private Long pedidoId;

    @Schema(description = "UUID offline del pedido (si fue creado offline)", example = "uuid-ped-1")
    private String uuidOffline;

    @Schema(description = "Estado actual del pedido", example = "EN_PROCESO")
    private EstadoPedido estado;

    @Schema(description = "Datos del cliente destino")
    private ClienteDeliveryResponse cliente;

    @Schema(description = "Lineas de garrafas a entregar")
    @Builder.Default
    private List<DeliveryDetalleResponse> detalles = new ArrayList<>();

    @Schema(description = "Datos de la parada (vinculacion ruta + pedido)")
    private ParadaResumenResponse parada;
}
