package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "DTO para planificar una ruta de reparto asignando pedidos a un repartidor")
public class RutaPlanificarRequest {

    @NotNull(message = "El ID del repartidor es obligatorio")
    @Schema(description = "ID del usuario con rol REPARTIDOR al que se le asignara la ruta", example = "5")
    private Long repartidorId;

    @FutureOrPresent(message = "La fecha de reparto no puede ser en el pasado")
    @Schema(description = "Fecha en la que se realizara el reparto", example = "2026-07-21")
    private LocalDate fechaReparto;

    @NotEmpty(message = "Debe enviar al menos un pedido para planificar")
    @Schema(description = "IDs de los pedidos que conformaran la ruta", example = "[42, 43, 44]")
    private List<Long> pedidosIds;
}