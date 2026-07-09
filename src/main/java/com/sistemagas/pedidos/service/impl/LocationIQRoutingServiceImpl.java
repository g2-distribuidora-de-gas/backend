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
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class LocationIQRoutingServiceImpl implements RoutingService {

    private final RestTemplate restTemplate;
    private final LocationIQProperties properties;

    @Override
    public RouteResultDto calcularRutaOptimizada(BigDecimal origenLat, BigDecimal origenLng, List<Pedido> pedidos) {
        List<Pedido> pedidosRuteables = new ArrayList<>();
        List<Pedido> pedidosOmitidos = new ArrayList<>();
        
        for (Pedido p : pedidos) {
            if (p.getCliente() != null && p.getCliente().getLatitud() != null && p.getCliente().getLongitud() != null) {
                pedidosRuteables.add(p);
            } else {
                pedidosOmitidos.add(p);
                log.warn("El pedido {} no tiene coordenadas validas, se omite de la optimizacion", p.getId());
            }
        }

        if (pedidosRuteables.isEmpty()) {
            return crearRutaFallback(pedidos);
        }

        try {
            StringBuilder coordsBuilder = new StringBuilder();
            coordsBuilder.append(origenLng).append(",").append(origenLat);

            for (Pedido p : pedidosRuteables) {
                coordsBuilder.append(";")
                        .append(p.getCliente().getLongitud())
                        .append(",")
                        .append(p.getCliente().getLatitud());
            }

            String url = UriComponentsBuilder.fromHttpUrl(properties.getOptimizationUrl() + coordsBuilder.toString())
                    .queryParam("key", properties.getApiKey())
                    .queryParam("source", "first")
                    .queryParam("roundtrip", "false")
                    .queryParam("steps", "false")
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
            if (body != null && "Ok".equals(body.get("code"))) {
                List<Map<String, Object>> trips = (List<Map<String, Object>>) body.get("trips");
                List<Map<String, Object>> waypointsInfo = (List<Map<String, Object>>) body.get("waypoints");

                if (trips != null && !trips.isEmpty() && waypointsInfo != null) {
                    Map<String, Object> bestTrip = trips.get(0);
                    
                    Double distance = Optional.ofNullable(bestTrip.get("distance")).map(Number.class::cast).map(Number::doubleValue).orElse(0.0);
                    Double duration = Optional.ofNullable(bestTrip.get("duration")).map(Number.class::cast).map(Number::doubleValue).orElse(0.0);
                    String geometry = Objects.toString(bestTrip.get("geometry"), "");
                    
                    List<Map<String, Object>> legs = (List<Map<String, Object>>) bestTrip.get("legs");
                    List<RouteResultDto.RouteWaypointDto> paradas = new ArrayList<>();
                    
                    RouteResultDto.RouteWaypointDto[] orderedStops = new RouteResultDto.RouteWaypointDto[pedidosRuteables.size()];
                    
                    // index 0 is origin, so we start from index 1 which corresponds to orders
                    for (int i = 1; i < waypointsInfo.size(); i++) {
                        Map<String, Object> wp = waypointsInfo.get(i);
                        int waypointIndex = Optional.ofNullable(wp.get("waypoint_index")).map(Number.class::cast).map(Number::intValue).orElse(i);
                        
                        Pedido p = pedidosRuteables.get(i - 1);
                        
                        int distLeg = 0;
                        int durLeg = 0;
                        // legs are 0-indexed corresponding to the leg leading to waypointIndex
                        if (legs != null && (waypointIndex - 1) >= 0 && (waypointIndex - 1) < legs.size()) {
                            Map<String, Object> leg = legs.get(waypointIndex - 1);
                            distLeg = Optional.ofNullable(leg.get("distance")).map(Number.class::cast).map(Number::intValue).orElse(0);
                            durLeg = Optional.ofNullable(leg.get("duration")).map(Number.class::cast).map(Number::intValue).orElse(0);
                        }
                        
                        if (waypointIndex > 0 && waypointIndex <= pedidosRuteables.size()) {
                            orderedStops[waypointIndex - 1] = RouteResultDto.RouteWaypointDto.builder()
                                    .pedidoId(p.getId())
                                    .orden(waypointIndex)
                                    .distanciaDesdeAnteriorM(distLeg)
                                    .duracionDesdeAnteriorS(durLeg)
                                    .build();
                        }
                    }
                    
                    for (RouteResultDto.RouteWaypointDto stop : orderedStops) {
                        if (stop != null) {
                            paradas.add(stop);
                        }
                    }
                    
                    // Add omitted orders at the end
                    for (Pedido p : pedidosOmitidos) {
                        paradas.add(RouteResultDto.RouteWaypointDto.builder()
                                .pedidoId(p.getId())
                                .orden(paradas.size() + 1)
                                .distanciaDesdeAnteriorM(0)
                                .duracionDesdeAnteriorS(0)
                                .build());
                    }

                    return RouteResultDto.builder()
                            .distanciaTotalM(distance.intValue())
                            .duracionTotalS(duration.intValue())
                            .geometria(geometry)
                            .paradasOrdenadas(paradas)
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Error al planificar ruta con LocationIQ API de Optimizacion", e);
        }

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
