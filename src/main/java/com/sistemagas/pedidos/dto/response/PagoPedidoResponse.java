package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Representación serializable de un {@code PagoPedido}. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoPedidoResponse {

    private Long id;
    private Long rutaPedidoId;

    private BigDecimal totalPedido;
    private BigDecimal montoEfectivo;
    private BigDecimal montoTransferencia;
    /** Saldo aún no cobrado: {@code totalPedido - montoEfectivo - montoTransferencia}. */
    private BigDecimal saldoPendiente;

    private EstadoPago estadoPago;
    private String motivoPendiente;

    private Long cobradoPorId;
    private String cobradoPorNombre;

    private String urlComprobante;


    private Instant createdAt;
    private Instant updatedAt;
}
