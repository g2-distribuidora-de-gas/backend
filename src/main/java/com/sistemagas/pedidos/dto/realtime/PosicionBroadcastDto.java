package com.sistemagas.pedidos.dto.realtime;

import com.sistemagas.pedidos.enums.EstadoRuta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Posicion geografica que el server retransmite al panel admin en el " +
        "topico /topic/rutas/{rutaId}/posiciones. Incluye el sello de tiempo del server " +
        "para que el front pueda calcular frescura de la muestra.")
public class PosicionBroadcastDto {

    @Schema(description = "ID de la ruta", example = "1")
    private Long rutaId;

    @Schema(description = "ID del repartidor que envia la posicion", example = "5")
    private Long repartidorId;

    @Schema(description = "Nombre y apellido del repartidor", example = "Juan Perez")
    private String repartidorNombre;

    @Schema(description = "Estado actual de la ruta al momento de emitir", example = "EN_CURSO")
    private EstadoRuta estadoRuta;

    @Schema(description = "Latitud en grados decimales (WGS84)", example = "-26.2072404")
    private BigDecimal latitud;

    @Schema(description = "Longitud en grados decimales (WGS84)", example = "-58.2123249")
    private BigDecimal longitud;

    @Schema(description = "Heading/rumbo del dispositivo en grados [0,360). Opcional.",
            example = "125.5")
    private BigDecimal headingGrados;

    @Schema(description = "Velocidad del dispositivo en metros por segundo. Opcional.",
            example = "8.4")
    private BigDecimal velocidadMps;

    @Schema(description = "Precision horizontal del GPS en metros. Opcional.", example = "5.0")
    private BigDecimal precisionM;

    @Schema(description = "Instante de la muestra en el reloj del cliente (UTC)",
            example = "2026-07-20T13:45:00Z")
    private Instant timestampCliente;

    @Schema(description = "Instante en que el server retransmitio la muestra (UTC)",
            example = "2026-07-20T13:45:00.420Z")
    private Instant serverTimestamp;

    @Schema(description = "Origen de la coordenada", example = "GPS",
            allowableValues = {"GPS", "NETWORK", "MANUAL"})
    private String origen;
}
