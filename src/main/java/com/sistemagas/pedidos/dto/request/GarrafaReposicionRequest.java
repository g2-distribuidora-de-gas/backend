package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para reponer stock de una Garrafa")
public class GarrafaReposicionRequest {

    @NotNull(message = "La cantidad no puede ser nula")
    @Min(value = 1, message = "La cantidad a reponer debe ser mayor o igual a 1")
    @Schema(description = "Cantidad de unidades a reponer al stock", example = "50")
    private Integer cantidad;

    @Size(max = 200, message = "El motivo no puede tener más de 200 caracteres")
    @Schema(description = "Motivo o nota opcional sobre la reposicion", example = "Reposicion mensual")
    private String motivo;
}