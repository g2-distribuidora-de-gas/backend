package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.EstadoRuta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO para registrar un cambio de estado de una ruta generado offline")
public class SincronizacionRutaItemRequest {

    @NotNull(message = "El ID de la ruta es obligatorio")
    @Schema(description = "ID de la ruta", example = "5")
    private Long rutaId;

    @NotBlank(message = "El uuidOffline es obligatorio para identificar este evento")
    @Schema(description = "UUID generado offline para identificar este cambio", example = "ruta-evt-001")
    private String uuidOffline;

    @NotNull(message = "El nuevo estado de la ruta es obligatorio")
    @Schema(description = "Nuevo estado de la ruta", example = "EN_CURSO")
    private EstadoRuta nuevoEstado;
}