package com.sistemagas.pedidos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumen de cobro para la pantalla de pago del repartidor.
 * Incluye el total calculado, el desglose por tipo de garrafa
 * y el cobro ya existente (si lo hay).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenCobroResponse {

    private Long rutaPedidoId;

    /** Total a cobrar, calculado en base a cantidadEntregada × precioUnitario. */
    private BigDecimal totalPedido;

    /** Desglose por ítem del pedido. */
    private List<ItemCobroResponse> detalles;

    /**
     * Cobro ya registrado para esta parada.
     * {@code null} si aún no se registró ningún cobro.
     */
    private PagoPedidoResponse pagoExistente;

    /** Ítem de detalle de cobro: una línea por tipo de garrafa entregada. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemCobroResponse {
        private Long pedidoDetalleId;
        private Long tipoGarrafaId;
        private String garrafaTipo;
        private Integer cantidadSolicitada;
        private Integer cantidadEntregada;
        private BigDecimal precioUnitario;
        /** subtotal histórico persistido en PedidoDetalle (precio × cantidad solicitada). */
        private BigDecimal subtotalHistorico;
        /** subtotal real de la entrega: precioUnitario × cantidadEntregada. */
        private BigDecimal subtotalEntregado;
    }
}
