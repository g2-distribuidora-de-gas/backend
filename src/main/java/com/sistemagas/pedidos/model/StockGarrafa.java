package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.exception.StockInsuficienteException;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stock actual de garrafas agrupado por (depósito, tipo, estado).
 *
 * <p>La cantidad NUNCA puede ser negativa. El campo {@code version} habilita
 * Optimistic Locking: JPA lanzará {@code ObjectOptimisticLockingFailureException}
 * si dos transacciones intentan modificar la misma fila simultáneamente.</p>
 *
 * <p>No existe tabla de "stock general". El stock total del sistema es
 * siempre la suma de todos los registros de esta tabla.</p>
 */
@Entity
@Table(
    name = "stock_garrafa",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_stock_deposito_tipo_estado",
        columnNames = {"deposito_id", "tipo_garrafa_id", "estado_garrafa_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockGarrafa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposito_id", nullable = false)
    private Deposito deposito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_garrafa_id", nullable = false)
    private TipoGarrafaStock tipoGarrafa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_garrafa_id", nullable = false)
    private EstadoGarrafa estadoGarrafa;

    /**
     * Cantidad actual de garrafas. Nunca puede ser negativa.
     * Validado tanto a nivel de BD (CHECK constraint) como en la capa de servicio.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 0;

    /**
     * Versión para Optimistic Locking gestionado por JPA.
     * NO modificar manualmente.
     */
    @Version
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void actualizarTimestamp() {
        this.updatedAt = Instant.now();
    }

    // ----------------------------------------------------------------
    // Métodos de dominio
    // ----------------------------------------------------------------

    /**
     * Incrementa la cantidad de forma segura.
     *
     * @param delta cantidad a sumar (debe ser > 0)
     */
    public void incrementar(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("El delta de incremento debe ser positivo, se recibió: " + delta);
        }
        this.cantidad += delta;
    }

    /**
     * Decrementa la cantidad validando que no quede negativa.
     *
     * @param delta cantidad a restar (debe ser > 0)
     * @throws StockInsuficienteException si no hay suficiente stock
     */
    public void decrementar(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("El delta de decremento debe ser positivo, se recibió: " + delta);
        }
        if (this.cantidad < delta) {
            throw new StockInsuficienteException(
                "Stock insuficiente en depósito [" + deposito.getNombre() + "] " +
                "para tipo [" + tipoGarrafa.getCodigo() + "] " +
                "estado [" + estadoGarrafa.getCodigo() + "]. " +
                "Disponible: " + this.cantidad + ", solicitado: " + delta
            );
        }
        this.cantidad -= delta;
    }
}
