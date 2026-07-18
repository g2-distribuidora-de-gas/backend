package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "pedido_detalles",
        indexes = {
                @Index(name = "idx_detalle_pedido", columnList = "pedido_id"),
                @Index(name = "idx_detalle_garrafa", columnList = "garrafa_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoDetalle extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @NotNull(message = "El ID de tipo garrafa es obligatorio")
    @Column(name = "tipo_garrafa_id", nullable = false)
    private Long tipoGarrafaId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Min(value = 0, message = "La cantidad entregada no puede ser negativa")
    @Column(name = "cantidad_entregada")
    private Integer cantidadEntregada;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Version
    private Long version;

    // subtotal se calcula UNA SOLA VEZ en @PrePersist y NO se modifica en updates,
    // para preservar el valor historico de venta al confirmar entregas parciales.
    // Si cantidadEntregada cambia, no debe recalcularse.
    @Column(name = "subtotal", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @PrePersist
    public void calcularSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            throw new IllegalStateException(
                    "precioUnitario y cantidad son requeridos para calcular subtotal del PedidoDetalle");
        }
        this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}