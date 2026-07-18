package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "depositos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deposito extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoDeposito tipo;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Patente del vehículo. Solo aplica cuando tipo = CAMION.
     */
    @Column(name = "vehiculo_patente", length = 20)
    private String vehiculoPatente;

    /**
     * Repartidor asignado al camión. Solo aplica cuando tipo = CAMION.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private Usuario repartidor;
}
