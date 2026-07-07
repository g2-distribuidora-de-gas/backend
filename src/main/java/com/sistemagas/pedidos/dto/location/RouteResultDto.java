package com.sistemagas.pedidos.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResultDto {
    private Integer distanciaTotalM;
    private Integer duracionTotalS;
    private String geometria; // polyline
    // Lista de paradas en el orden óptimo devuelto por la API
    private List<RouteWaypointDto> paradasOrdenadas; 
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteWaypointDto {
        private Long pedidoId;
        private Integer orden;
        private Integer distanciaDesdeAnteriorM;
        private Integer duracionDesdeAnteriorS;
    }
}
