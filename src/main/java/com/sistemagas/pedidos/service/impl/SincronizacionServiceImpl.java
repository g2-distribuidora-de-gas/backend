package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.UsuarioModel;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.repository.port.UsuarioRepositoryPort;
import com.sistemagas.pedidos.service.SincronizacionService;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionServiceImpl implements SincronizacionService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final GarrafaRepositoryPort garrafaRepositoryPort;
    private final PedidoMapper pedidoMapper;
    private final GarrafaStockHelper garrafaStockHelper;

    @Override
    @Transactional
    public SincronizacionResponse procesarPedidosOffline(SincronizacionRequest request) {
        log.info("Iniciando sincronizacion offline. Pedidos recibidos: {}", request.getPedidos().size());

        SincronizacionResponse response = SincronizacionResponse.builder()
                .servidorFecha(Instant.now())
                .total(request.getPedidos().size())
                .procesados(new ArrayList<>())
                .duplicados(new ArrayList<>())
                .errores(new ArrayList<>())
                .build();

        List<String> uuids = request.getPedidos().stream()
                .map(PedidoRequest::getUuidOffline)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .toList();

        Map<String, Pedido> existentes = uuids.isEmpty()
                ? Map.of()
                : pedidoRepository.findByUuidOfflineIn(uuids).stream()
                        .collect(Collectors.toMap(Pedido::getUuidOffline, Function.identity()));

        for (PedidoRequest pedidoReq : request.getPedidos()) {
            try {
                procesarPedidoIndividual(pedidoReq, existentes, response);
            } catch (Exception ex) {
                log.error("Error procesando pedido uuidOffline={}: {}",
                        pedidoReq.getUuidOffline(), ex.getMessage(), ex);
                response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                        .uuidOffline(pedidoReq.getUuidOffline())
                        .motivo("Error inesperado: " + ex.getMessage())
                        .build());
            }
        }

        log.info("Sincronizacion finalizada. Procesados: {}, Duplicados: {}, Errores: {}",
                response.getProcesados().size(),
                response.getDuplicados().size(),
                response.getErrores().size());

        return response;
    }

    private void procesarPedidoIndividual(PedidoRequest request,
                                           Map<String, Pedido> existentes,
                                           SincronizacionResponse response) {

        if (request.getUuidOffline() == null || request.getUuidOffline().isBlank()) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .motivo("uuidOffline es obligatorio para sincronizacion offline")
                    .build());
            return;
        }

        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo("El pedido debe tener al menos un detalle")
                    .build());
            return;
        }

        if (existentes.containsKey(request.getUuidOffline())) {
            response.getDuplicados().add(request.getUuidOffline());
            return;
        }

        UsuarioModel usuario = usuarioRepositoryPort.findById(request.getUsuarioId()).orElse(null);
        if (usuario == null) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo("UsuarioModel no encontrado: id=" + request.getUsuarioId())
                    .build());
            return;
        }

        Map<Long, GarrafaModel> garrafas;
        try {
            garrafas = garrafaStockHelper.cargarYValidar(request.getDetalles());
        } catch (Exception ex) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo(ex.getMessage())
                    .build());
            return;
        }

        Pedido pedido = pedidoMapper.toEntity(request);
        pedido.setUuidOffline(request.getUuidOffline());
        pedido.setUsuarioId(usuario.getId());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        for (PedidoDetalleRequest det : request.getDetalles()) {
            GarrafaModel garrafa = garrafas.get(det.getGarrafaId());
            BigDecimal precioUnitario = garrafa.getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(det.getCantidad()));

            PedidoDetalle detalle = PedidoDetalle.builder()
                    .garrafaId(garrafa.getId())
                    .cantidad(det.getCantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .build();

            pedido.agregarDetalle(detalle);

            garrafa.setStockDisponible(garrafa.getStockDisponible() - det.getCantidad());
            garrafaRepositoryPort.save(garrafa);
        }

        Pedido guardado = pedidoRepository.save(pedido);

        log.info("Pedido sincronizado: id={}, uuidOffline={}, detalles={}",
                guardado.getId(), guardado.getUuidOffline(), guardado.getDetalles().size());

        response.getProcesados().add(SincronizacionResponse.Procesado.builder()
                .uuidOffline(request.getUuidOffline())
                .pedidoId(guardado.getId())
                .build());
    }
}