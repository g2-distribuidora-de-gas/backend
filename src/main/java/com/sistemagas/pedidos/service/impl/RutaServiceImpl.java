package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.location.RouteResultDto;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.RoutingService;
import com.sistemagas.pedidos.service.RutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;
    private final RutaPedidoRepository rutaPedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoutingService routingService;

    // Coordenadas base del depósito (se podría configurar en la BD o
    // application.yml)
    private static final BigDecimal DEPOSITO_LAT = new BigDecimal("-26.2072404");
    private static final BigDecimal DEPOSITO_LNG = new BigDecimal("-58.2123249");

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
        RouteResultDto optimizacion = routingService.calcularRutaOptimizada(DEPOSITO_LAT, DEPOSITO_LNG, pedidos);

        // 2. Armar la Ruta maestra
        Ruta nuevaRuta = Ruta.builder()
                .fechaReparto(LocalDate.now())
                .repartidor(repartidor)
                .origenLat(DEPOSITO_LAT)
                .origenLng(DEPOSITO_LNG)
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
        return rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                repartidorId, LocalDate.now(), EstadoRuta.EN_CURSO)
                .orElseGet(() -> rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                        repartidorId, LocalDate.now(), EstadoRuta.PLANIFICADA)
                        .orElseThrow(() -> new ResourceNotFoundException("No hay ruta activa para el repartidor hoy")));
    }

    @Override
    @Transactional
    public void actualizarEstadoParada(Long rutaPedidoId, EstadoEntrega nuevoEstado) {
        RutaPedido parada = rutaPedidoRepository.findById(rutaPedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parada no encontrada"));

        EstadoEntrega actual = parada.getEstadoEntrega();
        if (actual != EstadoEntrega.PENDIENTE) {
            throw new BusinessException(
                    "No se puede cambiar el estado de una parada " + actual
                            + " (solo se permite cambiar desde PENDIENTE)");
        }

        parada.setEstadoEntrega(nuevoEstado);
        rutaPedidoRepository.save(parada);

        // Actualizamos también el pedido padre si es necesario
        Pedido pedido = parada.getPedido();
        if (nuevoEstado == EstadoEntrega.ENTREGADO) {
            pedido.setEstado(EstadoPedido.ENTREGADO);
        } else if (nuevoEstado == EstadoEntrega.FALLIDO) {
            pedido.setEstado(EstadoPedido.CANCELADO); // O un estado equivalente
        }
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
    public Ruta cambiarEstadoRuta(Long rutaId, EstadoRuta nuevoEstado) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada"));
        ruta.setEstado(nuevoEstado);
        return rutaRepository.save(ruta);
    }
}
