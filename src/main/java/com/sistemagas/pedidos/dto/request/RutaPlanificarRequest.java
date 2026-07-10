package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO para planificar una ruta de reparto asignando pedidos a un repartidor")
public class RutaPlanificarRequest {

    @NotNull(message = "El ID del repartidor es obligatorio")
    @Schema(description = "ID del usuario con rol REPARTIDOR al que se le asignara la ruta", example = "5")
    private Long repartidorId;

    @NotEmpty(message = "Debe enviar al menos un pedido para planificar")
    @Schema(description = "IDs de los pedidos que conformaran la ruta", example = "[42, 43, 44]")
    private List<Long> pedidosIds;
}