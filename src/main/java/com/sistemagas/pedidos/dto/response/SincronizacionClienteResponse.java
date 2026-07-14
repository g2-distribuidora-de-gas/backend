package com.sistemagas.pedidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resultado del procesamiento del lote de clientes offline enviados a sincronizar")
public class SincronizacionClienteResponse {

    @Schema(description = "Cantidad total de clientes recibidos en el lote", example = "5")
    private Integer total;

    @Schema(description = "Momento en que el servidor proceso la sincronizacion (UTC)",
            example = "2026-01-15T10:30:00Z")
    private Instant servidorFecha;

    @Schema(description = "Clientes creados exitosamente en el servidor")
    @Builder.Default
    private List<Procesado> procesados = new ArrayList<>();

    @Schema(description = "UUIDs de clientes que ya existian en el servidor (idempotencia)",
            example = "[\"uuid-ya-sincronizado\"]")
    @Builder.Default
    private List<String> duplicados = new ArrayList<>();

    @Schema(description = "Clientes que no pudieron procesarse con el motivo del error")
    @Builder.Default
    private List<ErrorItem> errores = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Cliente creado exitosamente durante la sincronizacion")
    public static class Procesado {
        @Schema(description = "UUID offline del cliente", example = "uuid-123")
        private String uuidOffline;

        @Schema(description = "ID asignado en el servidor", example = "42")
        private Long clienteId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Cliente que fallo durante la sincronizacion")
    public static class ErrorItem {
        @Schema(description = "UUID offline del cliente que fallo", example = "uuid-456")
        private String uuidOffline;

        @Schema(description = "Motivo del error", example = "Error de validacion")
        private String motivo;
    }
}
