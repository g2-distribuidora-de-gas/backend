package com.sistemagas.pedidos.dto.realtime;

import com.sistemagas.pedidos.enums.EstadoRuta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notificacion de un cambio de estado o parada sobre una ruta. Se emite " +
        "en /topic/rutas/{rutaId}/eventos para que el panel admin reaccione sin polling.")
public class EventoRutaWsDto {

    @Schema(description = "Tipo de evento", example = "CAMBIO_ESTADO_RUTA",
            allowableValues = {"CAMBIO_ESTADO_RUTA", "CAMBIO_ESTADO_PARADA", "RUTA_CANCELADA"})
    private String tipo;

    @Schema(description = "ID de la ruta afectada", example = "1")
    private Long rutaId;

    @Schema(description = "ID del repartidor dueno de la ruta", example = "5")
    private Long repartidorId;

    @Schema(description = "Estado previo (para cambios de estado). Null en otros tipos.",
            example = "EN_CURSO")
    private EstadoRuta estadoAnterior;

    @Schema(description = "Estado nuevo (para cambios de estado). Null en otros tipos.",
            example = "COMPLETADA")
    private EstadoRuta estadoNuevo;

    @Schema(description = "ID de la parada afectada (para CAMBIO_ESTADO_PARADA). Null en otros.",
            example = "10")
    private Long rutaPedidoId;

    @Schema(description = "Mensaje libre", example = "Ruta marcada como COMPLETADA")
    private String mensaje;

    @Schema(description = "Instante del evento (UTC)", example = "2026-07-20T13:45:00Z")
    private Instant timestamp;
}
