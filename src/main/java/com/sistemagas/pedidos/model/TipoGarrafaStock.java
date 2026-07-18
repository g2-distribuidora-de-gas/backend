package com.sistemagas.pedidos.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_garrafa_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoGarrafaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código corto único. Ej: "10KG", "15KG", "45KG".
     * Coincide con el nombre del enum TipoGarrafa del módulo de pedidos.
     */
    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String descripcion;

    @Column(name = "capacidad_kg", nullable = false)
    private Integer capacidadKg;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal precio = java.math.BigDecimal.ZERO;
}
