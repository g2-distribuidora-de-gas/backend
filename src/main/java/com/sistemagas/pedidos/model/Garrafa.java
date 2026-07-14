package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.exception.BusinessException;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.http.HttpStatus;

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

    @NotNull(message = "El tipo de garrafa es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TipoGarrafa tipo;

    @NotNull(message = "La capacidad en kg es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    @Column(name = "capacidad_kg", nullable = false)
    private Integer capacidadKg;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @NotNull(message = "El stock disponible es obligatorio")
    @Min(value = 0, message = "El stock disponible no puede ser negativo")
    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(nullable = false)
    private boolean activo;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    private void validarCapacidad() {
        if (tipo == null) {
            throw new BusinessException(
                    "El tipo de garrafa es obligatorio",
                    HttpStatus.BAD_REQUEST,
                    "GARRAFA_TIPO_OBLIGATORIO");
        }
        if (capacidadKg == null) {
            throw new BusinessException(
                    "La capacidad en kg es obligatoria",
                    HttpStatus.BAD_REQUEST,
                    "GARRAFA_CAPACIDAD_OBLIGATORIA");
        }
        if (tipo.getCapacidadKg() != capacidadKg) {
            throw new BusinessException(
                    "La capacidad " + capacidadKg + "kg no coincide con el tipo " + tipo
                            + " (deberia ser " + tipo.getCapacidadKg() + "kg)",
                    HttpStatus.BAD_REQUEST,
                    "GARRAFA_CAPACIDAD_INCONSISTENTE");
        }
    }
}