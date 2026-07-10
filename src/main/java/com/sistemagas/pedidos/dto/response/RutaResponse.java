package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoRuta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@Schema(description = "Respuesta con el detalle de una ruta planificada")
public class RutaResponse {

    @Schema(description = "ID de la ruta", example = "1")
    private Long id;

    @Schema(description = "Fecha en la que se realizara el reparto", example = "2026-01-20")
    private LocalDate fechaReparto;

    @Schema(description = "ID del repartidor asignado", example = "5")
    private Long repartidorId;

    @Schema(description = "Latitud del punto de origen de la ruta", example = "-34.603722")
    private BigDecimal origenLat;

    @Schema(description = "Longitud del punto de origen de la ruta", example = "-58.381592")
    private BigDecimal origenLng;

    @Schema(description = "Distancia total de la ruta en metros", example = "12500")
    private Integer distanciaTotalM;

    @Schema(description = "Duracion estimada total de la ruta en segundos", example = "1800")
    private Integer duracionTotalS;

    @Schema(description = "Geometria de la polilinea de la ruta (formato del proveedor de routing)",
            example = "{\"type\":\"LineString\",\"coordinates\":[[...]]}")
    private String geometria;

    @Schema(description = "Estado actual de la ruta", example = "EN_CURSO")
    private EstadoRuta estado;

    @Schema(description = "Paradas (pedidos) que componen la ruta en orden de visita")
    private List<RutaPedidoResponse> paradas;
}