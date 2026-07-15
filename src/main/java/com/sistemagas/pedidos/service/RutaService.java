package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.dto.response.RutaReprogramadaResponse;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.Usuario;

import java.time.Instant;
import java.time.LocalDate;
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
    void actualizarEstadoParada(Long rutaPedidoId, ActualizarParadaRequest request, Usuario autenticado);

    /**
     * Cambia el estado general de la Ruta (ej: EN_CURSO a COMPLETADA)
     */
    Ruta cambiarEstadoRuta(Long rutaId, EstadoRuta nuevoEstado, Usuario autenticado);

    /**
     * Devuelve el detalle de un pedido en formato liviano para la app del repartidor,
     * asociado a su parada (RutaPedido). Aplica validacion de propiedad para REPARTIDOR;
     * ADMIN/SUPER_ADMIN pueden consultar cualquier parada.
     *
     * @throws com.sistemagas.pedidos.exception.ResourceNotFoundException si la parada no existe
     * @throws com.sistemagas.pedidos.exception.BusinessException si la parada pertenece a la ruta
     *         de otro repartidor y el usuario autenticado es REPARTIDOR
     */
    DeliveryReadOnlyResponse obtenerPedidoDeParada(Long rutaPedidoId, Usuario autenticado);

    /**
     * Lista rutas en estado REPROGRAMADA con detalle de paradas fallidas, ordenadas por
     * updatedAt ascendente y luego id ascendente.
     *
     * @param fechaDesde    limite inferior del rango de fechaReparto (inclusive). Si es null, default = hoy - 30 dias.
     * @param fechaHasta    limite superior del rango de fechaReparto (inclusive). Si es null, default = hoy.
     * @param repartidorId  filtro opcional por repartidor dueño.
     * @param minUpdatedAt  filtro opcional por updatedAt mayor o igual.
     * @param limit         maximo de rutas a retornar (1-1000). Si es null, default = 100.
     */
    List<RutaReprogramadaResponse> listarReprogramadas(LocalDate fechaDesde, LocalDate fechaHasta,
                                                       Long repartidorId, Instant minUpdatedAt, Integer limit);
}
