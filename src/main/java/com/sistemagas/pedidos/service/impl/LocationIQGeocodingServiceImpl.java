package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.LocationIQProperties;
import com.sistemagas.pedidos.dto.location.LocationDto;
import com.sistemagas.pedidos.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationIQGeocodingServiceImpl implements GeocodingService {

    private final RestTemplate restTemplate;
    private final LocationIQProperties properties;

    @Override
    public LocationDto obtenerCoordenadas(String direccion) {
        try {
            java.net.URI uri = UriComponentsBuilder.fromHttpUrl(properties.getGeocodingUrl())
                    .queryParam("key", properties.getApiKey())
                    .queryParam("q", direccion)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .build()
                    .toUri();

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> body = response.getBody();
            if (body != null && !body.isEmpty()) {
                Map<String, Object> bestMatch = body.get(0);
                
                Object placeIdObj = bestMatch.get("place_id");
                Object latObj = bestMatch.get("lat");
                Object lonObj = bestMatch.get("lon");
                Object precisionObj = bestMatch.get("class");
                
                String placeId = placeIdObj != null ? String.valueOf(placeIdObj) : null;
                String latStr = latObj != null ? String.valueOf(latObj) : null;
                String lonStr = lonObj != null ? String.valueOf(lonObj) : null;
                String precision = precisionObj != null ? String.valueOf(precisionObj) : null;

                return LocationDto.builder()
                        .placeId(placeId)
                        .latitud(latStr != null ? new BigDecimal(latStr) : null)
                        .longitud(lonStr != null ? new BigDecimal(lonStr) : null)
                        .precision(precision)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error al obtener coordenadas de LocationIQ para la direccion: {}", direccion, e);
        }
        return null;
    }
}
