package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
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
    private final GarrafaStockHelper garrafaStockHelper;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long guardar(PedidoRequest request, Cliente cliente, Map<Long, GarrafaModel> garrafas, String emailAutenticado) {
        Pedido pedido = pedidoMapper.toEntity(request);
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        
        Usuario creador = null;
        if (request.getCreadorId() != null) {
            creador = usuarioRepository.findById(request.getCreadorId()).orElse(null);
        } else if (emailAutenticado != null) {
            creador = usuarioRepository.findByEmail(emailAutenticado).orElse(null);
        }
        pedido.setCreador(creador);

        for (PedidoDetalleRequest det : request.getDetalles()) {
            GarrafaModel garrafa = garrafas.get(det.getGarrafaId());
            if (garrafa == null) {
                throw new IllegalStateException("Garrafa no encontrada: id=" + det.getGarrafaId());
            }
            BigDecimal precioUnitario = garrafa.getPrecio();

            PedidoDetalle detalle = PedidoDetalle.builder()
                    .garrafaId(garrafa.getId())
                    .cantidad(det.getCantidad())
                    .precioUnitario(precioUnitario)
                    .build();

            pedido.agregarDetalle(detalle);

            garrafaStockHelper.validarYDescontar(garrafa, det.getCantidad());
            garrafaRepositoryPort.save(garrafa);
        }

        return pedidoRepository.save(pedido).getId();
    }
}