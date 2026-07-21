package com.sistemagas.pedidos.dto.realtime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notificacion push enviada al repartidor cuando se le asigna o modifica un recorrido. " +
        "Se emite en /user/queue/agenda.")
public class AgendaNotificacionDto {

    @Schema(description = "Tipo de evento de agenda",
            allowableValues = {"NUEVA_RUTA_ASIGNADA", "RUTA_CANCELADA", "NOTAS_ACTUALIZADAS"},
            example = "NUEVA_RUTA_ASIGNADA")
    private String tipo;

    @Schema(description = "ID de la ruta afectada", example = "42")
    private Long rutaId;

    @Schema(description = "Fecha del recorrido afectado", example = "2026-07-25")
    private LocalDate fechaReparto;

    @Schema(description = "ID del repartidor destinatario", example = "5")
    private Long repartidorId;

    @Schema(description = "Mensaje legible para el usuario",
            example = "Se te asigno un recorrido para el 25/07/2026")
    private String mensaje;

    @Schema(description = "Instante del evento (UTC)", example = "2026-07-21T13:00:00Z")
    private Instant timestamp;
}
