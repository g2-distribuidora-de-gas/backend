package com.sistemagas.pedidos.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Envelope estandar de respuesta de la API")
public class ApiResponse<T> {

    @Schema(description = "Indica si la operacion fue exitosa", example = "true")
    private boolean exito;

    @Schema(description = "Mensaje legible para humanos sobre el resultado", example = "Operacion exitosa")
    private String mensaje;

    @Schema(description = "Payload con el resultado (puede ser null en operaciones sin retorno)")
    private T data;

    @Schema(description = "Momento en que se genero la respuesta (UTC)", example = "2026-01-15T10:30:00Z")
    private Instant timestamp;

    public static <T> ApiResponse<T> ok(T data, String mensaje) {
        return ApiResponse.<T>builder()
                .exito(true)
                .mensaje(mensaje)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Operacion exitosa");
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return ApiResponse.<T>builder()
                .exito(false)
                .mensaje(mensaje)
                .timestamp(Instant.now())
                .build();
    }
}