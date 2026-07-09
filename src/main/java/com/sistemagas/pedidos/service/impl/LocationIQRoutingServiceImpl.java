package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.LocationIQProperties;
import com.sistemagas.pedidos.dto.location.RouteResultDto;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.service.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class LocationIQRoutingServiceImpl implements RoutingService {

    private final RestTemplate restTemplate;
    private final LocationIQProperties properties;

    @Override
    public RouteResultDto calcularRutaOptimizada(BigDecimal origenLat, BigDecimal origenLng, List<Pedido> pedidos) {
        try {
            // LocationIQ espera coordenadas en el formato: {longitud},{latitud};{longitud},{latitud}...
            StringBuilder coordsBuilder = new StringBuilder();
            coordsBuilder.append(origenLng).append(",").append(origenLat);

            for (Pedido p : pedidos) {
                if (p.getCliente() != null && p.getCliente().getLatitud() != null) {
                    coordsBuilder.append(";")
                            .append(p.getCliente().getLongitud())
                            .append(",")
                            .append(p.getCliente().getLatitud());
                } else {
                    log.warn("El pedido {} no tiene coordenadas validas, se omite de la ruta geométrica", p.getId());
                }
            }

            String url = UriComponentsBuilder.fromHttpUrl(properties.getRoutingUrl() + coordsBuilder.toString())
                    .queryParam("key", properties.getApiKey())
                    .queryParam("steps", "true")
                    .queryParam("alternatives", "false")
                    .queryParam("geometries", "polyline")
                    .queryParam("overview", "full")
                    .toUriString();

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> bestRoute = routes.get(0);
                    
                    Double distance = ((Number) bestRoute.get("distance")).doubleValue();
                    Double duration = ((Number) bestRoute.get("duration")).doubleValue();
                    String geometry = (String) bestRoute.get("geometry");
                    
                    // Aquí simulamos el orden mapeando las "legs" devueltas con el array original de pedidos
                    List<RouteResultDto.RouteWaypointDto> waypoints = new ArrayList<>();
                    
                    List<Map<String, Object>> legs = (List<Map<String, Object>>) bestRoute.get("legs");
                    int i = 0;
                    for (Pedido p : pedidos) {
                        // Nota: el optimizador real (Routing/Optimization API de LocationIQ o OSRM) 
                        // puede reordenar. OSRM simple devuelve el mismo orden que le pasaste.
                        // Para optimización real TSP (Traveling Salesman Problem) usar endpoint de optimization.
                        int distLeg = (i < legs.size()) ? ((Number) legs.get(i).get("distance")).intValue() : 0;
                        int durLeg = (i < legs.size()) ? ((Number) legs.get(i).get("duration")).intValue() : 0;
                        
                        waypoints.add(RouteResultDto.RouteWaypointDto.builder()
                                .pedidoId(p.getId())
                                .orden(i + 1)
                                .distanciaDesdeAnteriorM(distLeg)
                                .duracionDesdeAnteriorS(durLeg)
                                .build());
                        i++;
                    }

                    return RouteResultDto.builder()
                            .distanciaTotalM(distance.intValue())
                            .duracionTotalS(duration.intValue())
                            .geometria(geometry)
                            .paradasOrdenadas(waypoints)
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Error al planificar ruta con LocationIQ", e);
        }

        // Fallback: Si falla la API, creamos una ruta vacía o ficticia para no bloquear el sistema
        return crearRutaFallback(pedidos);
    }
    
    private RouteResultDto crearRutaFallback(List<Pedido> pedidos) {
        List<RouteResultDto.RouteWaypointDto> paradas = new ArrayList<>();
        int i = 1;
        for (Pedido p : pedidos) {
            paradas.add(RouteResultDto.RouteWaypointDto.builder()
                    .pedidoId(p.getId())
                    .orden(i++)
                    .distanciaDesdeAnteriorM(0)
                    .duracionDesdeAnteriorS(0)
                    .build());
        }
        return RouteResultDto.builder()
                .distanciaTotalM(0)
                .duracionTotalS(0)
                .geometria("")
                .paradasOrdenadas(paradas)
                .build();
    }
}
