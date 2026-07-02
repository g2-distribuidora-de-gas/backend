package com.sistemagas.pedidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta para la consulta de estado de UUIDs ya sincronizados")
public class SincronizacionEstadoResponse {

    @Schema(description = "Cantidad total de UUIDs consultados", example = "3")
    private Integer totalConsultados;

    @Schema(description = "Cantidad de UUIDs que ya fueron procesados y existen en la base de datos",
            example = "2")
    private Integer encontrados;

    @Schema(description = "Lista de UUIDs que ya fueron procesados (existen en la base de datos)")
    @Builder.Default
    private List<String> procesados = new ArrayList<>();

    @Schema(description = "Lista de UUIDs consultados que aun no fueron procesados (no existen en la base de datos)")
    @Builder.Default
    private List<String> noEncontrados = new ArrayList<>();
}