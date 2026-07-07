package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoPedido;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponse {

    private Long id;
    private String uuidOffline;
    private Long clienteId;
    private String clienteNombre;
    private String direccionEntrega;
    private EstadoPedido estado;
    private BigDecimal total;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<PedidoDetalleResponse> detalles = new ArrayList<>();
}