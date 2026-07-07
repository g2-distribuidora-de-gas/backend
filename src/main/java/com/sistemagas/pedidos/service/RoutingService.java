package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.location.RouteResultDto;
import com.sistemagas.pedidos.model.Pedido;

import java.math.BigDecimal;
import java.util.List;

public interface RoutingService {
    
    /**
     * Calcula la ruta óptima desde un punto de origen hacia varios pedidos.
     * Devuelve la geometría general de la ruta y el orden de las paradas.
     */
    RouteResultDto calcularRutaOptimizada(BigDecimal origenLat, BigDecimal origenLng, List<Pedido> pedidos);
}
