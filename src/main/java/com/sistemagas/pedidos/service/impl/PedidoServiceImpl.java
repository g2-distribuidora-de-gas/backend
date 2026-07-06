package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoDetalleResponse;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapper;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.UsuarioModel;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.repository.port.UsuarioRepositoryPort;
import com.sistemagas.pedidos.service.PedidoService;
import com.sistemagas.pedidos.util.Constantes;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final GarrafaRepositoryPort garrafaRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PedidoMapper pedidoMapper;
    private final PedidoDetalleMapper pedidoDetalleMapper;
    private final GarrafaStockHelper garrafaStockHelper;

    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        if (request.getUuidOffline() != null
                && pedidoRepository.existsByUuidOffline(request.getUuidOffline())) {
            throw new BusinessException(Constantes.MSG_PEDIDO_DUPLICADO);
        }

        UsuarioModel usuario = usuarioRepositoryPort.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        Pedido pedido = pedidoMapper.toEntity(request);
        pedido.setUsuarioId(usuario.getId());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        Map<Long, GarrafaModel> garrafas = garrafaStockHelper.cargarYValidar(request.getDetalles());

        for (PedidoDetalleRequest det : request.getDetalles()) {
            GarrafaModel garrafa = garrafas.get(det.getGarrafaId());
            if (garrafa == null) {
                throw new ResourceNotFoundException(Constantes.MSG_GARRAFA_NO_ENCONTRADA + ": id=" + det.getGarrafaId());
            }
            BigDecimal precioUnitario = garrafa.getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(det.getCantidad()));

            PedidoDetalle detalle = PedidoDetalle.builder()
                    .garrafaId(garrafa.getId())
                    .cantidad(det.getCantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotal)
                    .build();

            pedido.agregarDetalle(detalle);

            garrafaStockHelper.validarYDescontar(garrafa, det.getCantidad());
            garrafaRepositoryPort.save(garrafa);
        }

        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido creado: id={}, uuidOffline={}, detalles={}",
                guardado.getId(), guardado.getUuidOffline(), guardado.getDetalles().size());

        return buildResponse(guardado, usuario, garrafas);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public PedidoResponse obtenerPorUuidOffline(String uuidOffline) {
        Pedido pedido = pedidoRepository.findByUuidOffline(uuidOffline)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));
        return buildResponseFor(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public PedidoResponse obtenerPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));
        return buildResponseFor(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public List<PedidoResponse> listarTodos(Instant minUpdatedAt, Integer limit) {
        int pageLimit = (limit != null && limit > 0) ? limit : 100;
        Pageable pageable = PageRequest.of(0, pageLimit, Sort.by("updatedAt").ascending().and(Sort.by("id").ascending()));
        
        List<Pedido> pedidos = (minUpdatedAt != null)
                ? pedidoRepository.findByUpdatedAtGreaterThan(minUpdatedAt, pageable)
                : pedidoRepository.findAllBy(pageable);

        return pedidos.stream()
                .map(this::buildResponseFor)
                .toList();
    }

    @Override
    @Transactional
    public void actualizarEstado(Long id, EstadoPedido estado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));
        pedido.setEstado(estado);
        pedidoRepository.save(pedido);
    }

    private PedidoResponse buildResponseFor(Pedido pedido) {
        UsuarioModel usuario = usuarioRepositoryPort.findById(pedido.getUsuarioId()).orElse(null);

        Map<Long, GarrafaModel> garrafas = new HashMap<>();
        for (PedidoDetalle d : pedido.getDetalles()) {
            garrafaRepositoryPort.findById(d.getGarrafaId()).ifPresent(g -> garrafas.put(g.getId(), g));
        }

        return buildResponse(pedido, usuario, garrafas);
    }

    private PedidoResponse buildResponse(Pedido pedido, UsuarioModel usuario, Map<Long, GarrafaModel> garrafas) {
        PedidoResponse response = pedidoMapper.toResponse(pedido);
        pedidoMapper.fillUsuarioNombreCompleto(response, pedido, usuario);

        List<PedidoDetalleResponse> detalles = new ArrayList<>();
        for (PedidoDetalle d : pedido.getDetalles()) {
            PedidoDetalleResponse detResp = pedidoDetalleMapper.toResponse(d);
            pedidoDetalleMapper.fillGarrafaTipo(detResp, garrafas.get(d.getGarrafaId()));
            detalles.add(detResp);
        }
        response.setDetalles(detalles);
        return response;
    }
}