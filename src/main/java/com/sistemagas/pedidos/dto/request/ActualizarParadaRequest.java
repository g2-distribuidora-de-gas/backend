package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "DTO para actualizar el estado de entrega de una parada de una ruta")
public class ActualizarParadaRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    @Schema(description = "Nuevo estado de la parada", example = "ENTREGADO")
    private EstadoEntrega nuevoEstado;

    @Schema(description = "Motivo por el cual no se pudo entregar, requerido si el estado es FALLIDO", example = "El cliente no estaba en el domicilio")
    private String motivoFallo;

    @Schema(description = "Lista de cantidades entregadas para entregas parciales. Si no se envía y el estado es ENTREGADO, se asume entrega completa.")
    private List<DetalleEntregaRequest> entregas;
}