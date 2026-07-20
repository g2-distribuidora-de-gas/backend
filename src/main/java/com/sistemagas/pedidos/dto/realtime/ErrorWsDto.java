package com.sistemagas.pedidos.dto.realtime;

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
@Schema(description = "Mensaje de error que el server envia al usuario por el destino " +
        "/user/queue/errors cuando una operacion STOMP falla (autorizacion, validacion, etc.).")
public class ErrorWsDto {

    @Schema(description = "Codigo legible para que el front reaccione sin parsear el mensaje",
            example = "RUTA_NO_TRANSMITE")
    private String codigo;

    @Schema(description = "Mensaje legible para humanos", example = "La ruta 1 esta COMPLETADA")
    private String mensaje;

    @Schema(description = "Instante en que se genero el error (UTC)",
            example = "2026-07-20T13:45:00Z")
    private Instant timestamp;
}
