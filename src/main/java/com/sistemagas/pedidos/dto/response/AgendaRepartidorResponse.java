package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.ConfirmacionRepartidor;
import com.sistemagas.pedidos.enums.EstadoRuta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Entrada de la agenda del repartidor: resumen de un recorrido asignado")
public class AgendaRepartidorResponse {

    @Schema(description = "ID de la ruta", example = "42")
    private Long rutaId;

    @Schema(description = "ID del repartidor asignado", example = "5")
    private Long repartidorId;

    @Schema(description = "Fecha del recorrido", example = "2026-07-25")
    private LocalDate fechaReparto;

    @Schema(description = "Estado actual del recorrido", example = "PLANIFICADA")
    private EstadoRuta estado;

    @Schema(description = "Cantidad total de paradas en el recorrido", example = "8")
    private int cantidadParadas;

    @Schema(description = "Paradas ya entregadas", example = "3")
    private int paradasEntregadas;

    @Schema(description = "Paradas fallidas", example = "1")
    private int paradasFallidas;

    @Schema(description = "Paradas aun pendientes", example = "4")
    private int paradasPendientes;

    @Schema(description = "Notas del administrador para este recorrido",
            example = "Llevar cambio de 5000. El cliente del punto 3 pide llegar antes de las 10.")
    private String notasAdmin;

    @Schema(description = "Estado de confirmacion del repartidor", example = "PENDIENTE")
    private ConfirmacionRepartidor confirmacionRepartidor;

    @Schema(description = "Distancia total del recorrido en metros", example = "34500")
    private Integer distanciaTotalM;

    @Schema(description = "Duracion estimada total en segundos", example = "5400")
    private Integer duracionTotalS;
}
