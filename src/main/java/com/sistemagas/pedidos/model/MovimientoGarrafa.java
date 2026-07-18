package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Registro INMUTABLE de un movimiento de stock.
 *
 * <p>Esta entidad nunca se modifica ni se elimina una vez persistida.
 * Es el ledger del sistema: toda operación que altere stock_garrafa
 * debe generar exactamente un registro aquí.</p>
 *
 * <ul>
 *   <li>deposito_origen_id  = null → ingreso sin origen (ajuste entrada, devolución)</li>
 *   <li>deposito_destino_id = null → salida definitiva (venta, rotura, ajuste salida)</li>
 *   <li>estado_origen_id    = null → no aplica cambio de estado previo</li>
 * </ul>
 */
@Entity
@Table(name = "movimientos_garrafa")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoGarrafa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private TipoMovimiento tipoMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_origen_id")
    private Deposito depositoOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposito_destino_id")
    private Deposito depositoDestino;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_garrafa_id", nullable = false)
    private TipoGarrafaStock tipoGarrafa;

    /**
     * Estado de la garrafa ANTES del movimiento. Null si es ingreso inicial.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_origen_id")
    private EstadoGarrafa estadoOrigen;

    /**
     * Estado de la garrafa DESPUÉS del movimiento. Siempre requerido.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_destino_id", nullable = false)
    private EstadoGarrafa estadoDestino;

    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Pedido relacionado. Opcional (ej: ventas originadas desde un pedido).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    /**
     * Usuario que realizó la operación. Obligatorio para trazabilidad.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Instant fecha;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    private void prePersist() {
        if (this.fecha == null) {
            this.fecha = Instant.now();
        }
    }
}
