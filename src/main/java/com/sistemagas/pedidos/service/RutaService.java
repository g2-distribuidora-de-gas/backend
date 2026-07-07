package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.Ruta;

import java.util.List;

public interface RutaService {
    
    /**
     * Genera y guarda una ruta optimizada llamando al RoutingService.
     */
    Ruta planificarRuta(Long repartidorId, List<Long> pedidosIds);
    
    /**
     * Obtiene la ruta actual de un repartidor
     */
    Ruta obtenerRutaActivaRepartidor(Long repartidorId);
    
    /**
     * Actualiza el estado de una parada específica (ej: PENDIENTE -> ENTREGADO)
     * y si todo está entregado, puede auto-completar la Ruta.
     */
    void actualizarEstadoParada(Long rutaPedidoId, EstadoEntrega nuevoEstado);
    
    /**
     * Cambia el estado general de la Ruta (ej: EN_CURSO a COMPLETADA)
     */
    Ruta cambiarEstadoRuta(Long rutaId, EstadoRuta nuevoEstado);
}
