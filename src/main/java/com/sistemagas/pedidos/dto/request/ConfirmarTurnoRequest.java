package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.ConfirmacionRepartidor;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request del repartidor para confirmar o rechazar un turno asignado")
public class ConfirmarTurnoRequest {

    @NotNull(message = "La confirmacion es obligatoria")
    @Schema(description = "CONFIRMADO o RECHAZADO", allowableValues = {"CONFIRMADO", "RECHAZADO"}, example = "CONFIRMADO")
    private ConfirmacionRepartidor confirmacion;

    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    @Schema(description = "Motivo del rechazo (requerido si confirmacion = RECHAZADO)", example = "No puedo trabajar ese dia")
    private String motivoRechazo;
}
