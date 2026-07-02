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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        log.info("Pedido creado: id={}, uuidOffline={}, detalles={}",
                guardado.getId(), guardado.getUuidOffline(), guardado.getDetalles().size());

        return buildResponse(guardado, usuario, garrafas);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorUuidOffline(String uuidOffline) {
        Pedido pedido = pedidoRepository.findByUuidOffline(uuidOffline)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));

        UsuarioModel usuario = usuarioRepositoryPort.findById(pedido.getUsuarioId())
                .orElse(null);

        Map<Long, GarrafaModel> garrafas = new java.util.HashMap<>();
        for (PedidoDetalle d : pedido.getDetalles()) {
            garrafaRepositoryPort.findById(d.getGarrafaId()).ifPresent(g -> garrafas.put(g.getId(), g));
        }

        return buildResponse(pedido, usuario, garrafas);
    }

    private PedidoResponse buildResponse(Pedido pedido, UsuarioModel usuario, Map<Long, GarrafaModel> garrafas) {
        PedidoResponse response = pedidoMapper.toResponse(pedido);
        pedidoMapper.fillUsuarioNombreCompleto(response, pedido, usuario);

        List<PedidoDetalleResponse> detalles = new java.util.ArrayList<>();
        for (PedidoDetalle d : pedido.getDetalles()) {
            PedidoDetalleResponse detResp = pedidoDetalleMapper.toResponse(d);
            pedidoDetalleMapper.fillGarrafaTipo(detResp, garrafas.get(d.getGarrafaId()));
            detalles.add(detResp);
        }
        response.setDetalles(detalles);
        return response;
    }
}