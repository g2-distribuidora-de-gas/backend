package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Lote de pedidos creados offline que el cliente envia para sincronizar contra el servidor")
public class SincronizacionRequest {

    @NotEmpty(message = "La lista de pedidos no puede estar vacia")
    @Valid
    @Schema(description = "Pedidos a sincronizar. Cada pedido debe incluir su uuidOffline generado en el cliente")
    private List<PedidoRequest> pedidos;
}