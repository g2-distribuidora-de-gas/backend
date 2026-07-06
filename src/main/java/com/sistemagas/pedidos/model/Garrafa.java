package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;

import com.sistemagas.pedidos.model.base.Auditable;

@Entity
@Table(name = "garrafas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Garrafa extends Auditable implements GarrafaModel {
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

    @Min(value = 0, message = "El stock disponible no puede ser negativo")
    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(nullable = false)
    private Boolean activo;
}