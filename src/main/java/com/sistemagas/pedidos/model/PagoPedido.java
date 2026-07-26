package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.EstadoPago;
import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Registro de cobro asociado a una parada de ruta ({@link RutaPedido}).
 *
 * <p>Cada parada tiene <b>como máximo un</b> cobro (UNIQUE en {@code ruta_pedido_id}).
 *
 * <p>El {@link EstadoPago} se deriva automáticamente en la capa de servicio:
 * <ul>
 *   <li>efectivo + transferencia == total → {@link EstadoPago#PAGADO}</li>
 *   <li>0 < suma < total             → {@link EstadoPago#PARCIAL}</li>
 *   <li>suma == 0                    → {@link EstadoPago#PENDIENTE} (requiere motivoPendiente)</li>
 * </ul>
 */
@Entity
@Table(
        name = "pagos_pedido",
        indexes = {
                @Index(name = "idx_pagos_estado",      columnList = "estado_pago"),
                @Index(name = "idx_pagos_cobrado_por", columnList = "cobrado_por_id"),
                @Index(name = "idx_pagos_ruta_pedido", columnList = "ruta_pedido_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoPedido extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parada a la que corresponde este cobro. Relación 1-a-1. */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_pedido_id", nullable = false, unique = true)
    private RutaPedido rutaPedido;

    /** Total que debería haber pagado el cliente (calculado al momento del cobro). */
    @NotNull
    @DecimalMin("0.00")
    @Column(name = "total_pedido", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPedido;

    /** Monto cobrado en efectivo. */
    @NotNull
    @DecimalMin("0.00")
    @Builder.Default
    @Column(name = "monto_efectivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoEfectivo = BigDecimal.ZERO;

    /** Monto cobrado por transferencia bancaria. */
    @NotNull
    @DecimalMin("0.00")
    @Builder.Default
    @Column(name = "monto_transferencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTransferencia = BigDecimal.ZERO;

    /**
     * Saldo aún pendiente de cobro.
     * Desnormalizado: {@code totalPedido - montoEfectivo - montoTransferencia}.
     */
    @NotNull
    @DecimalMin("0.00")
    @Builder.Default
    @Column(name = "saldo_pendiente", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    /**
     * Estado derivado del cobro.
     * Se calcula en {@code CobroServiceImpl} y se persiste para facilitar queries de pendientes.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;

    /**
     * Motivo por el que el cobro quedó en PENDIENTE o PARCIAL.
     * Obligatorio cuando {@link #estadoPago} == {@link EstadoPago#PENDIENTE}.
     * Opcional para {@link EstadoPago#PARCIAL}.
     */
    @Column(name = "motivo_pendiente")
    private String motivoPendiente;

    /**
     * URL / Object Path del comprobante de transferencia en Supabase Storage.
     * Obligatorio si montoTransferencia > 0.
     */
    @Column(name = "url_comprobante")
    private String urlComprobante;


    /** Repartidor (u operador) que registró el cobro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobrado_por_id")
    private Usuario cobradoPor;
}
