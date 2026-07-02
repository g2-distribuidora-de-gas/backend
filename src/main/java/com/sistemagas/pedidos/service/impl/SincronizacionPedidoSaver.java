package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.UsuarioModel;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SincronizacionPedidoSaver {

    private final PedidoRepository pedidoRepository;
    private final GarrafaRepositoryPort garrafaRepositoryPort;
    private final PedidoMapper pedidoMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long guardar(PedidoRequest request, UsuarioModel usuario, Map<Long, GarrafaModel> garrafas) {
        Pedido pedido = pedidoMapper.toEntity(request);
        pedido.setUuidOffline(request.getUuidOffline());
        pedido.setUsuarioId(usuario.getId());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        for (PedidoDetalleRequest det : request.getDetalles()) {
            GarrafaModel garrafa = garrafas.get(det.getGarrafaId());
            if (garrafa == null) {
                throw new IllegalStateException("Garrafa no encontrada: id=" + det.getGarrafaId());
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

            garrafa.setStockDisponible(garrafa.getStockDisponible() - det.getCantidad());
            garrafaRepositoryPort.save(garrafa);
        }

        return pedidoRepository.save(pedido).getId();
    }
}