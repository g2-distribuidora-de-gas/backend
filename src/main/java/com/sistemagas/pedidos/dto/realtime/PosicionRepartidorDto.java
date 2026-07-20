package com.sistemagas.pedidos.dto.realtime;

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
@Schema(description = "Posicion geografica enviada por el movil del repartidor al servidor. " +
        "Es el payload que viaja por STOMP al destino /app/rutas/{rutaId}/posicion.")
public class PosicionRepartidorDto {

    @Schema(description = "ID de la ruta a la que pertenece el reparto", example = "1")
    private Long rutaId;

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

    @Schema(description = "Instante en que el GPS del cliente tomo la muestra (UTC)",
            example = "2026-07-20T13:45:00Z")
    private Instant timestampCliente;

    @Schema(description = "Origen de la coordenada", example = "GPS",
            allowableValues = {"GPS", "NETWORK", "MANUAL"})
    private String origen;
}
