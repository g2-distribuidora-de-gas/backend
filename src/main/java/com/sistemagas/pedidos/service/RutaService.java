package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.ActualizarNotasAdminRequest;
import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.request.ConfirmarTurnoRequest;
import com.sistemagas.pedidos.dto.response.AgendaRepartidorResponse;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.dto.response.RutaReprogramadaResponse;
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
    Ruta planificarRuta(Long repartidorId, List<Long> pedidosIds, LocalDate fechaReparto);

    /**
     * Obtiene la ruta activa de hoy de un repartidor.
     */
    Ruta obtenerRutaActivaRepartidor(Long repartidorId);

    /**
     * Actualiza el estado de una parada específica (ej: PENDIENTE -> ENTREGADO)
     * y si todo está resuelto puede auto-completar la Ruta.
     */
    void actualizarEstadoParada(Long rutaPedidoId, ActualizarParadaRequest request, Usuario autenticado);

    /**
     * Cambia el estado general de la Ruta (ej: EN_CURSO a COMPLETADA).
     */
    Ruta cambiarEstadoRuta(Long rutaId, EstadoRuta nuevoEstado, Usuario autenticado);

    /**
     * Devuelve el detalle de un pedido en formato liviano para la app del repartidor.
     * Aplica validacion de propiedad para REPARTIDOR; ADMIN/SUPER_ADMIN ven cualquier parada.
     */
    DeliveryReadOnlyResponse obtenerPedidoDeParada(Long rutaPedidoId, Usuario autenticado);

    /**
     * Lista rutas en estado REPROGRAMADA con detalle de paradas fallidas.
     */
    List<RutaReprogramadaResponse> listarReprogramadas(LocalDate fechaDesde, LocalDate fechaHasta,
                                                       Long repartidorId, Instant minUpdatedAt, Integer limit);

    /**
     * Lista todas las rutas (cualquier estado) en un rango de fechas. Panel del admin.
     */
    List<Ruta> listarTodas(LocalDate fechaDesde, LocalDate fechaHasta,
                           Long repartidorId, Integer limit);

    // ─── Agenda ──────────────────────────────────────────────────────────────────

    /**
     * Devuelve la agenda del repartidor: rutas ordenadas por fecha_reparto ASC
     * dentro del rango indicado. Defaults si null: fechaDesde=hoy, fechaHasta=hoy+30d.
     */
    List<AgendaRepartidorResponse> obtenerAgendaRepartidor(
            Long repartidorId, LocalDate fechaDesde, LocalDate fechaHasta);
    /**
     * Devuelve la agenda global de todos los repartidores.
     */
    List<AgendaRepartidorResponse> obtenerAgendaGlobal(LocalDate fechaDesde, LocalDate fechaHasta);
    /**
     * El repartidor confirma o rechaza un turno asignado.
     * Solo puede ejecutarlo el propietario de la ruta.
     *
     * @throws com.sistemagas.pedidos.exception.BusinessException si la ruta no le pertenece
     *         o si el estado de confirmacion ya es terminal.
     */
    AgendaRepartidorResponse confirmarTurno(Long rutaId, ConfirmarTurnoRequest request, Usuario autenticado);

    /**
     * El admin actualiza las notas de un recorrido.
     * Emite una notificacion WebSocket al repartidor via /user/queue/agenda.
     */
    AgendaRepartidorResponse actualizarNotasAdmin(Long rutaId, ActualizarNotasAdminRequest request);
}
