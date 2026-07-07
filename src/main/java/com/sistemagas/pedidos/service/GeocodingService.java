package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.location.LocationDto;

public interface GeocodingService {
    
    /**
     * Obtiene las coordenadas y detalles geográficos a partir de una dirección de texto.
     */
    LocationDto obtenerCoordenadas(String direccion);
}
