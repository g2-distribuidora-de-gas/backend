package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.DepositoProperties;
import com.sistemagas.pedidos.dto.location.RouteResultDto;
import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.response.ClienteDeliveryResponse;
import com.sistemagas.pedidos.dto.response.DeliveryDetalleResponse;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.dto.response.ParadaResumenResponse;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.service.RoutingService;
import com.sistemagas.pedidos.service.RutaService;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final RutaPedidoRepository rutaPedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoutingService routingService;
    private final GarrafaStockHelper garrafaStockHelper;
    private final DepositoProperties depositoProperties;
    private final GarrafaRepositoryPort garrafaRepositoryPort;
    private final SupabaseStorageService supabaseStorageService;

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
        List<Ruta> enCurso = rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                repartidorId, LocalDate.now(), EstadoRuta.EN_CURSO);
        if (!enCurso.isEmpty()) {
            if (enCurso.size() > 1) {
                log.warn("El repartidor {} tiene {} rutas en EN_CURSO hoy. Tomando la primera (id={})",
                        repartidorId, enCurso.size(), enCurso.get(0).getId());
            }
            return enCurso.get(0);
        }
        List<Ruta> planificadas = rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                repartidorId, LocalDate.now(), EstadoRuta.PLANIFICADA);
        if (!planificadas.isEmpty()) {
            if (planificadas.size() > 1) {
                log.warn("El repartidor {} tiene {} rutas PLANIFICADAS hoy. Tomando la primera (id={})",
                        repartidorId, planificadas.size(), planificadas.get(0).getId());
            }
            return planificadas.get(0);
        }
        throw new ResourceNotFoundException("No hay ruta activa para el repartidor hoy");
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
                    "No se puede cambiar el estado de una parada " + actual
                            + " (solo se permite cambiar desde PENDIENTE)");
        }

        EstadoEntrega nuevoEstado = request.getNuevoEstado();
        parada.setEstadoEntrega(nuevoEstado);

        // Actualizamos también el pedido padre si es necesario
        Pedido pedido = parada.getPedido();
        if (nuevoEstado == EstadoEntrega.ENTREGADO) {
            pedido.setEstado(EstadoPedido.ENTREGADO);
            
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
                        
                        int noEntregadas = detalle.getCantidad() - (cantEntregada != null ? cantEntregada : 0);
                        if (noEntregadas > 0) {
                            garrafaStockHelper.restituirStock(detalle.getGarrafaId(), noEntregadas);
                        }
                    } else {
                        detalle.setCantidadEntregada(detalle.getCantidad());
                    }
                }
            } else {
                for (com.sistemagas.pedidos.model.PedidoDetalle detalle : pedido.getDetalles()) {
                    detalle.setCantidadEntregada(detalle.getCantidad());
                }
            }
        } else if (nuevoEstado == EstadoEntrega.FALLIDO) {
            parada.setMotivoFallo(request.getMotivoFallo());
            pedido.setEstado(EstadoPedido.REPROGRAMADO);
            
            // Si falló, restituir todo el stock reservado
            for (com.sistemagas.pedidos.model.PedidoDetalle detalle : pedido.getDetalles()) {
                garrafaStockHelper.restituirStock(detalle.getGarrafaId(), detalle.getCantidad());
            }
        }
        
        rutaPedidoRepository.save(parada);
        pedidoRepository.save(pedido);

        // Verificar si la ruta entera fue completada
        Ruta ruta = parada.getRuta();
        boolean todoEntregado = ruta.getParadas().stream()
                .allMatch(p -> p.getEstadoEntrega() != EstadoEntrega.PENDIENTE);

        if (todoEntregado) {
            ruta.setEstado(EstadoRuta.COMPLETADA);
            rutaRepository.save(ruta);
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

        ruta.setEstado(nuevoEstado);
        return rutaRepository.save(ruta);
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
            com.sistemagas.pedidos.enums.TipoGarrafa tipo = null;
            GarrafaModel garrafa = garrafaRepositoryPort.findById(d.getGarrafaId()).orElse(null);
            if (garrafa != null && garrafa.getTipo() != null) {
                tipo = garrafa.getTipo();
            }

            result.add(DeliveryDetalleResponse.builder()
                    .id(d.getId())
                    .garrafaId(d.getGarrafaId())
                    .garrafaTipo(tipo)
                    .cantidad(d.getCantidad())
                    .cantidadEntregada(d.getCantidadEntregada())
                    .precioUnitario(d.getPrecioUnitario())
                    .subtotal(d.getSubtotal())
                    .build());
        }
        return result;
    }
}
