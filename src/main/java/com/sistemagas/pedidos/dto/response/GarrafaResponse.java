package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de respuesta para Garrafa")
public class GarrafaResponse {

    @Schema(description = "ID de la garrafa", example = "1")
    private Long id;

    @Schema(description = "Tipo de garrafa", example = "GARRAFA_10KG")
    private TipoGarrafa tipo;

    @Schema(description = "Capacidad en KG", example = "10")
    private Integer capacidadKg;

    @Schema(description = "Precio de la garrafa", example = "15000.00")
    private BigDecimal precio;

    @Schema(description = "Stock disponible", example = "100")
    private Integer stockDisponible;

    @Schema(description = "Indica si la garrafa está activa", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de última modificación")
    private java.time.Instant updatedAt;
}
