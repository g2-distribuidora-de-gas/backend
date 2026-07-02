package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para la creación/actualización de una Garrafa")
public class GarrafaRequest {

    @NotNull(message = "El tipo de garrafa no puede ser nulo")
    @Schema(description = "Tipo de garrafa", example = "GARRAFA_10KG")
    private TipoGarrafa tipo;

    @NotNull(message = "La capacidad en kg no puede ser nula")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    @Schema(description = "Capacidad en KG", example = "10")
    private Integer capacidadKg;

    @NotNull(message = "El precio no puede ser nulo")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Schema(description = "Precio de la garrafa", example = "15000.00")
    private BigDecimal precio;

    @NotNull(message = "El stock disponible no puede ser nulo")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Schema(description = "Stock disponible", example = "100")
    private Integer stockDisponible;

    @Schema(description = "Estado de la garrafa", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean activo = true;
}
