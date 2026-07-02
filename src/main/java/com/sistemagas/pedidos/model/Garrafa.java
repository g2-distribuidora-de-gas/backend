package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "garrafas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Garrafa implements GarrafaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TipoGarrafa tipo;

    @Column(name = "capacidad_kg", nullable = false)
    private Integer capacidadKg;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(nullable = false)
    private Boolean activo;
}