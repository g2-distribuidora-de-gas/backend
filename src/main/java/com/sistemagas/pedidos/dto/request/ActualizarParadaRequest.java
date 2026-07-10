package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO para actualizar el estado de entrega de una parada de una ruta")
public class ActualizarParadaRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    @Schema(description = "Nuevo estado de la parada", example = "ENTREGADO")
    private EstadoEntrega nuevoEstado;
}