package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.DepositoProperties;
import com.sistemagas.pedidos.dto.location.RouteResultDto;
import com.sistemagas.pedidos.dto.realtime.EventoRutaWsDto;
import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.response.ClienteDeliveryResponse;
import com.sistemagas.pedidos.dto.response.ClienteResumenResponse;
import com.sistemagas.pedidos.dto.response.DeliveryDetalleResponse;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.dto.response.DetalleResumenResponse;
import com.sistemagas.pedidos.dto.response.ParadaFalloResponse;
import com.sistemagas.pedidos.dto.response.ParadaResumenResponse;
import com.sistemagas.pedidos.dto.response.PedidoResumenResponse;
import com.sistemagas.pedidos.dto.response.RutaReprogramadaResponse;
import com.sistemagas.pedidos.dto.response.UsuarioResumenRepartidor;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.RoutingService;
import com.sistemagas.pedidos.service.RutaService;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import com.sistemagas.pedidos.service.TrackingService;
import com.sistemagas.pedidos.dto.request.VentaStockRequest;
import com.sistemagas.pedidos.service.InventarioService;
import com.sistemagas.pedidos.repository.DepositoRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.util.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final RutaPedidoRepository rutaPedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoutingService routingService;
    private final InventarioService inventarioService;
    private final DepositoRepository depositoRepository;
    private final TipoGarrafaStockRepository tipoGarrafaStockRepository;
    private final DepositoProperties depositoProperties;
    private final SupabaseStorageService supabaseStorageService;
    private final TrackingService trackingService;

    /**
     * Mapa de transiciones permitidas para {@link EstadoRuta}.
     * - PLANIFICADA: arranca o se cancela antes de empezar.
     * - EN_CURSO:     se completa, se cancela, o pasa a REPROGRAMADA (caso de day-off).
     * - COMPLETADA / CANCELADA: terminales.
     * - REPROGRAMADA: se retoma (EN_CURSO) o se cancela.
     */
    private static final Map<EstadoRuta, Set<EstadoRuta>> TRANSICIONES_RUTA = Map.of(
            EstadoRuta.PLANIFICADA,  Set.of(EstadoRuta.EN_CURSO, EstadoRuta.CANCELADA),
            EstadoRuta.EN_CURSO,      Set.of(EstadoRuta.COMPLETADA, EstadoRuta.CANCELADA, EstadoRuta.REPROGRAMADA),
            EstadoRuta.COMPLETADA,    Set.of(),
            EstadoRuta.CANCELADA,     Set.of(),
            EstadoRuta.REPROGRAMADA,  Set.of(EstadoRuta.EN_CURSO, EstadoRuta.CANCELADA)
    );

    // Coordenadas del deposito, parametrizables via app.routing.deposito.lat/lng
    // (ver DepositoProperties). Default: Formosa, Argentina.
    private BigDecimal depositoLat() {
        return new BigDecimal(depositoProperties.getLat());
    }

    private BigDecimal depositoLng() {
        return new BigDecimal(depositoProperties.getLng());
    }

    @Override
    @Transactional
    public Ruta planificarRuta(Long repartidorId, List<Long> pedidosIds) {
        Usuario repartidor = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));

        List<Pedido> pedidos = pedidoRepository.findAllById(pedidosIds);
        if (pedidos.isEmpty()) {
            throw new IllegalArgumentException("No se enviaron pedidos válidos");
        }

        // 1. Llamar a RoutingService
        RouteResultDto optimizacion = routingService.calcularRutaOptimizada(depositoLat(), depositoLng(), pedidos);

        // 2. Armar la Ruta maestra
        Ruta nuevaRuta = Ruta.builder()
                .fechaReparto(LocalDate.now())
                .repartidor(repartidor)
                .origenLat(depositoLat())
                .origenLng(depositoLng())
                .distanciaTotalM(optimizacion.getDistanciaTotalM())
                .duracionTotalS(optimizacion.getDuracionTotalS())
                .geometria(optimizacion.getGeometria())
                .estado(EstadoRuta.PLANIFICADA)
                .build();

        // 3. Crear paradas (RutaPedidos) en base al orden devuelto por la API
        for (RouteResultDto.RouteWaypointDto w : optimizacion.getParadasOrdenadas()) {
            Pedido ped = pedidos.stream().filter(p -> p.getId().equals(w.getPedidoId())).findFirst().orElseThrow();

            // Marcar el pedido principal como asignado a una ruta
            ped.setEstado(EstadoPedido.EN_PROCESO);

            RutaPedido parada = RutaPedido.builder()
                    .pedido(ped)
                    .orden(w.getOrden())
                    .distanciaDesdeAnteriorM(w.getDistanciaDesdeAnteriorM())
                    .duracionDesdeAnteriorS(w.getDuracionDesdeAnteriorS())
                    .estadoEntrega(EstadoEntrega.PENDIENTE)
                    .build();

            nuevaRuta.agregarParada(parada);
        }

        // Guardará en cascada ruta_pedidos y se actualizan los estados de pedidos
        return rutaRepository.save(nuevaRuta);
    }

    @Override
    public Ruta obtenerRutaActivaRepartidor(Long repartidorId) {
        List<EstadoRuta> estadosActivos = List.of(
                EstadoRuta.EN_CURSO,
                EstadoRuta.PLANIFICADA,
                EstadoRuta.REPROGRAMADA);

        List<Ruta> activas = rutaRepository.findByRepartidorIdAndFechaRepartoAndEstadoIn(
                repartidorId, LocalDate.now(), estadosActivos);

        if (activas.isEmpty()) {
            throw new ResourceNotFoundException("No hay ruta activa para el repartidor hoy");
        }

        if (activas.size() > 1) {
            log.warn("El repartidor {} tiene {} rutas activas hoy. "
                            + "Aplicando prioridad: EN_CURSO > PLANIFICADA > REPROGRAMADA.",
                    repartidorId, activas.size());
        }

        return activas.stream()
                .filter(r -> r.getEstado() == EstadoRuta.EN_CURSO)
                .findFirst()
                .or(() -> activas.stream()
                        .filter(r -> r.getEstado() == EstadoRuta.PLANIFICADA)
                        .findFirst())
                .or(() -> activas.stream()
                        .filter(r -> r.getEstado() == EstadoRuta.REPROGRAMADA)
                        .findFirst())
                .orElseThrow();
    }

    @Override
    @Transactional
    public void actualizarEstadoParada(Long rutaPedidoId, ActualizarParadaRequest request, Usuario autenticado) {
        RutaPedido parada = rutaPedidoRepository.findById(rutaPedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parada no encontrada"));

        if (autenticado.getRol().name().equals("REPARTIDOR")) {
            Long duenoId = parada.getRuta().getRepartidor().getId();
            if (!duenoId.equals(autenticado.getId())) {
                throw new BusinessException("No puedes modificar paradas de otro repartidor");
            }
        }

        EstadoEntrega actual = parada.getEstadoEntrega();
        if (actual != EstadoEntrega.PENDIENTE) {
            throw new BusinessException(
                    String.format(Constantes.MSG_ESTADO_NO_CAMBIABLE, actual));
        }

        EstadoEntrega nuevoEstado = request.getNuevoEstado();
        parada.setEstadoEntrega(nuevoEstado);

        // Actualizamos también el pedido padre si es necesario
        Pedido pedido = parada.getPedido();
        if (nuevoEstado == EstadoEntrega.ENTREGADO) {
            pedido.setEstado(EstadoPedido.ENTREGADO);
            
            Deposito camion = depositoRepository.findByRepartidorIdAndActivoTrue(parada.getRuta().getRepartidor().getId())
                    .orElseThrow(() -> new BusinessException("El repartidor no tiene un camión activo asignado"));

            // Entregas parciales
            if (request.getEntregas() != null && !request.getEntregas().isEmpty()) {
                Map<Long, Integer> entregasMap = request.getEntregas().stream()
                        .collect(Collectors.toMap(
                                com.sistemagas.pedidos.dto.request.DetalleEntregaRequest::getPedidoDetalleId,
                                com.sistemagas.pedidos.dto.request.DetalleEntregaRequest::getCantidadEntregada));
                
                for (com.sistemagas.pedidos.model.PedidoDetalle detalle : pedido.getDetalles()) {
                    if (entregasMap.containsKey(detalle.getId())) {
                        Integer cantEntregada = entregasMap.get(detalle.getId());
                        detalle.setCantidadEntregada(cantEntregada);
                    } else {
                        detalle.setCantidadEntregada(detalle.getCantidad());
                    }
                }
            } else {
                for (com.sistemagas.pedidos.model.PedidoDetalle detalle : pedido.getDetalles()) {
                    detalle.setCantidadEntregada(detalle.getCantidad());
                }
            }

            // Registrar ventas en el motor de stock
            for (com.sistemagas.pedidos.model.PedidoDetalle detalle : pedido.getDetalles()) {
                if (detalle.getCantidadEntregada() != null && detalle.getCantidadEntregada() > 0) {
                    TipoGarrafaStock tipoStock = tipoGarrafaStockRepository.findById(detalle.getTipoGarrafaId())
                            .orElseThrow(() -> new BusinessException("Tipo de garrafa de stock no encontrado: id=" + detalle.getTipoGarrafaId()));
                    
                    VentaStockRequest ventaReq = VentaStockRequest.builder()
                            .camionId(camion.getId())
                            .tipoGarrafaId(tipoStock.getId())
                            .cantidadEntregadas(detalle.getCantidadEntregada())
                            .cantidadRecibidas(detalle.getCantidadEntregada()) // Asumimos devolución 1 a 1 por ahora
                            .pedidoId(pedido.getId())
                            .observaciones("Venta desde app repartidor en parada " + parada.getId())
                            .build();
                    
                    inventarioService.registrarVenta(ventaReq, autenticado);
                }
            }

        } else if (nuevoEstado == EstadoEntrega.FALLIDO) {
            parada.setMotivoFallo(request.getMotivoFallo());
            pedido.setEstado(EstadoPedido.REPROGRAMADO);
            // Ya no es necesario restituir stock genérico, el stock sigue en el camión
        }
        
        rutaPedidoRepository.save(parada);
        pedidoRepository.save(pedido);

        // Verificar si la ruta entera fue procesada (todas las paradas resueltas)
        Ruta ruta = parada.getRuta();

        trackingService.emitirEventoRuta(
                ruta,
                ruta.getEstado(),
                EventoRutaWsDto.builder()
                        .tipo("CAMBIO_ESTADO_PARADA")
                        .rutaPedidoId(parada.getId())
                        .mensaje("Parada " + parada.getId() + " marcada como " + nuevoEstado)
                        .build());

        boolean todasProcesadas = ruta.getParadas().stream()
                .allMatch(p -> p.getEstadoEntrega() != EstadoEntrega.PENDIENTE);

        if (todasProcesadas) {
            long totalEntregadas = ruta.getParadas().stream()
                    .filter(p -> p.getEstadoEntrega() == EstadoEntrega.ENTREGADO).count();
            long totalFallidas = ruta.getParadas().stream()
                    .filter(p -> p.getEstadoEntrega() == EstadoEntrega.FALLIDO).count();

            EstadoRuta nuevoEstadoRuta;
            if (totalFallidas == 0) {
                nuevoEstadoRuta = EstadoRuta.COMPLETADA;
            } else if (totalEntregadas == 0) {
                nuevoEstadoRuta = EstadoRuta.REPROGRAMADA;
            } else {
                nuevoEstadoRuta = EstadoRuta.COMPLETADA;
            }

            // Solo aplicar si la transición está permitida (no pisar CANCELADA u otro estado terminal).
            if (TRANSICIONES_RUTA.getOrDefault(ruta.getEstado(), Set.of())
                    .contains(nuevoEstadoRuta)) {
                EstadoRuta estadoAnteriorRuta = ruta.getEstado();
                ruta.setEstado(nuevoEstadoRuta);
                rutaRepository.save(ruta);

                trackingService.emitirEventoRuta(
                        ruta,
                        estadoAnteriorRuta,
                        EventoRutaWsDto.builder()
                                .tipo("CAMBIO_ESTADO_RUTA")
                                .estadoAnterior(estadoAnteriorRuta)
                                .estadoNuevo(nuevoEstadoRuta)
                                .mensaje("Ruta " + ruta.getId()
                                        + " auto-completada al cerrar todas las paradas")
                                .build());
            }
        }
    }

    @Override
    @Transactional
    public Ruta cambiarEstadoRuta(Long rutaId, EstadoRuta nuevoEstado, Usuario autenticado) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada"));

        if (autenticado.getRol().name().equals("REPARTIDOR")) {
            if (!ruta.getRepartidor().getId().equals(autenticado.getId())) {
                throw new BusinessException("No puedes cambiar el estado de una ruta de otro repartidor");
            }
        }

        Set<EstadoRuta> destinosPermitidos =
                TRANSICIONES_RUTA.getOrDefault(ruta.getEstado(), Set.of());
        if (!destinosPermitidos.contains(nuevoEstado)) {
            throw new BusinessException(
                    String.format(Constantes.MSG_TRANSICION_RUTA_INVALIDA,
                            ruta.getEstado(), nuevoEstado),
                    HttpStatus.BAD_REQUEST,
                    "TRANSICION_ESTADO_RUTA_INVALIDA");
        }

        EstadoRuta estadoAnterior = ruta.getEstado();
        ruta.setEstado(nuevoEstado);
        Ruta rutaGuardada = rutaRepository.save(ruta);

        trackingService.emitirEventoRuta(
                rutaGuardada,
                estadoAnterior,
                EventoRutaWsDto.builder()
                        .tipo("CAMBIO_ESTADO_RUTA")
                        .estadoAnterior(estadoAnterior)
                        .estadoNuevo(nuevoEstado)
                        .mensaje("Ruta " + rutaId + " cambio de " + estadoAnterior + " a " + nuevoEstado)
                        .build());

        return rutaGuardada;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryReadOnlyResponse obtenerPedidoDeParada(Long rutaPedidoId, Usuario autenticado) {
        RutaPedido parada = rutaPedidoRepository.findById(rutaPedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parada no encontrada"));

        if (autenticado != null
                && "REPARTIDOR".equals(autenticado.getRol().name())
                && !parada.getRuta().getRepartidor().getId().equals(autenticado.getId())) {
            throw new BusinessException(
                    "No puedes ver pedidos de otro repartidor",
                    HttpStatus.FORBIDDEN,
                    "PARADA_NO_AUTORIZADA");
        }

        Pedido pedido = parada.getPedido();
        Cliente cliente = pedido.getCliente();

        ClienteDeliveryResponse clienteDto = mapClienteParaDelivery(cliente);
        List<DeliveryDetalleResponse> detallesDto = mapDetallesParaDelivery(pedido.getDetalles());

        ParadaResumenResponse paradaDto = ParadaResumenResponse.builder()
                .rutaPedidoId(parada.getId())
                .orden(parada.getOrden())
                .distanciaDesdeAnteriorM(parada.getDistanciaDesdeAnteriorM())
                .duracionDesdeAnteriorS(parada.getDuracionDesdeAnteriorS())
                .horaEstimadaLlegada(parada.getHoraEstimadaLlegada())
                .estadoEntrega(parada.getEstadoEntrega())
                .motivoFallo(parada.getMotivoFallo())
                .build();

        return DeliveryReadOnlyResponse.builder()
                .pedidoId(pedido.getId())
                .uuidOffline(pedido.getUuidOffline())
                .estado(pedido.getEstado())
                .cliente(clienteDto)
                .detalles(detallesDto)
                .parada(paradaDto)
                .build();
    }

    private ClienteDeliveryResponse mapClienteParaDelivery(Cliente cliente) {
        if (cliente == null) return null;

        String urlFoto = null;
        if (cliente.getFotoEvidenciaPath() != null) {
            try {
                urlFoto = supabaseStorageService.getSignedUrl(cliente.getFotoEvidenciaPath());
            } catch (RuntimeException ex) {
                log.warn("Fallo generando signed URL para la foto del cliente {}. Se devuelve sin foto.",
                        cliente.getId(), ex);
            }
        }

        return ClienteDeliveryResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .latitud(cliente.getLatitud())
                .longitud(cliente.getLongitud())
                .urlFotoEvidencia(urlFoto)
                .build();
    }

    private List<DeliveryDetalleResponse> mapDetallesParaDelivery(List<PedidoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) return new ArrayList<>();
        List<DeliveryDetalleResponse> result = new ArrayList<>();
        for (PedidoDetalle d : detalles) {
            String tipo = null;
            TipoGarrafaStock garrafa = tipoGarrafaStockRepository.findById(d.getTipoGarrafaId()).orElse(null);
            if (garrafa != null) {
                tipo = garrafa.getCodigo();
            }

            result.add(DeliveryDetalleResponse.builder()
                    .id(d.getId())
                    .tipoGarrafaId(d.getTipoGarrafaId())
                    .garrafaTipo(tipo)
                    .cantidad(d.getCantidad())
                    .cantidadEntregada(d.getCantidadEntregada())
                    .precioUnitario(d.getPrecioUnitario())
                    .subtotal(d.getSubtotal())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RutaReprogramadaResponse> listarReprogramadas(LocalDate fechaDesde, LocalDate fechaHasta,
                                                             Long repartidorId, Instant minUpdatedAt, Integer limit) {
        LocalDate desde = (fechaDesde != null) ? fechaDesde : LocalDate.now().minusDays(30);
        LocalDate hasta = (fechaHasta != null) ? fechaHasta : LocalDate.now();
        int pageLimit = (limit != null && limit > 0) ? limit : 100;
        Pageable pageable = PageRequest.of(0, pageLimit,
                Sort.by("updatedAt").ascending().and(Sort.by("id").ascending()));

        List<Ruta> rutas;
        if (repartidorId != null) {
            rutas = rutaRepository.findByRepartidorIdAndEstadoAndFechaRepartoBetween(
                    repartidorId, EstadoRuta.REPROGRAMADA, desde, hasta, pageable);
        } else if (minUpdatedAt != null) {
            rutas = rutaRepository.findByEstadoAndUpdatedAtGreaterThan(
                    EstadoRuta.REPROGRAMADA, minUpdatedAt, pageable);
        } else {
            rutas = rutaRepository.findByEstadoAndFechaRepartoBetween(
                    EstadoRuta.REPROGRAMADA, desde, hasta, pageable);
        }

        return rutas.stream()
                .map(this::mapRutaReprogramada)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ruta> listarTodas(LocalDate fechaDesde, LocalDate fechaHasta,
                                  Long repartidorId, Integer limit) {
        LocalDate desde = (fechaDesde != null) ? fechaDesde : LocalDate.now().minusDays(30);
        LocalDate hasta = (fechaHasta != null) ? fechaHasta : LocalDate.now();
        int pageLimit = (limit != null && limit > 0) ? limit : 100;
        Pageable pageable = PageRequest.of(0, pageLimit,
                Sort.by("updatedAt").descending().and(Sort.by("id").descending()));

        if (repartidorId != null) {
            return rutaRepository.findByRepartidorIdAndFechaRepartoBetween(
                    repartidorId, desde, hasta, pageable);
        }
        return rutaRepository.findByFechaRepartoBetween(desde, hasta, pageable);
    }

    private RutaReprogramadaResponse mapRutaReprogramada(Ruta ruta) {
        List<RutaPedido> paradas = ruta.getParadas() != null ? ruta.getParadas() : List.of();

        int totalEntregadas = (int) paradas.stream()
                .filter(p -> p.getEstadoEntrega() == EstadoEntrega.ENTREGADO).count();
        int totalFallidas = (int) paradas.stream()
                .filter(p -> p.getEstadoEntrega() == EstadoEntrega.FALLIDO).count();
        int totalPendientes = (int) paradas.stream()
                .filter(p -> p.getEstadoEntrega() == EstadoEntrega.PENDIENTE).count();

        List<ParadaFalloResponse> fallos = paradas.stream()
                .filter(p -> p.getEstadoEntrega() == EstadoEntrega.FALLIDO)
                .sorted((a, b) -> Integer.compare(
                        a.getOrden() != null ? a.getOrden() : 0,
                        b.getOrden() != null ? b.getOrden() : 0))
                .map(this::mapParadaFallo)
                .toList();

        return RutaReprogramadaResponse.builder()
                .rutaId(ruta.getId())
                .fechaReparto(ruta.getFechaReparto())
                .estado(ruta.getEstado())
                .repartidor(mapRepartidorResumen(ruta.getRepartidor()))
                .updatedAt(ruta.getUpdatedAt())
                .totalParadas(paradas.size())
                .totalEntregadas(totalEntregadas)
                .totalFallidas(totalFallidas)
                .totalPendientes(totalPendientes)
                .paradasFallidas(fallos)
                .build();
    }

    private ParadaFalloResponse mapParadaFallo(RutaPedido parada) {
        Pedido pedido = parada.getPedido();
        return ParadaFalloResponse.builder()
                .rutaPedidoId(parada.getId())
                .orden(parada.getOrden())
                .motivoFallo(parada.getMotivoFallo())
                .pedido(mapPedidoResumen(pedido))
                .build();
    }

    private PedidoResumenResponse mapPedidoResumen(Pedido pedido) {
        if (pedido == null) return null;
        return PedidoResumenResponse.builder()
                .pedidoId(pedido.getId())
                .uuidOffline(pedido.getUuidOffline())
                .estado(pedido.getEstado())
                .cliente(mapClienteResumen(pedido.getCliente()))
                .detalles(mapDetallesResumen(pedido.getDetalles()))
                .build();
    }

    private ClienteResumenResponse mapClienteResumen(Cliente cliente) {
        if (cliente == null) return null;
        return ClienteResumenResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .build();
    }

    private List<DetalleResumenResponse> mapDetallesResumen(List<PedidoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) return List.of();
        List<DetalleResumenResponse> result = new ArrayList<>();
        for (PedidoDetalle d : detalles) {
            result.add(DetalleResumenResponse.builder()
                    .pedidoDetalleId(d.getId())
                    .cantidadSolicitada(d.getCantidad())
                    .cantidadEntregada(d.getCantidadEntregada())
                    .build());
        }
        return result;
    }

    private UsuarioResumenRepartidor mapRepartidorResumen(Usuario repartidor) {
        if (repartidor == null) return null;
        return UsuarioResumenRepartidor.builder()
                .id(repartidor.getId())
                .nombre(repartidor.getNombre())
                .email(repartidor.getEmail())
                .build();
    }
}
