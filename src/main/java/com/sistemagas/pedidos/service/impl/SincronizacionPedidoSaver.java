package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
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
    private final TipoGarrafaStockRepository tipoGarrafaStockRepository;
    private final PedidoMapper pedidoMapper;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public Long guardar(PedidoRequest request, Cliente cliente, Map<Long, TipoGarrafaStock> garrafas, String emailAutenticado) {
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
            TipoGarrafaStock garrafa = garrafas.get(det.getTipoGarrafaId());
            if (garrafa == null) {
                throw new IllegalStateException("Garrafa no encontrada: id=" + det.getTipoGarrafaId());
            }
            BigDecimal precioUnitario = garrafa.getPrecio();

            PedidoDetalle detalle = PedidoDetalle.builder()
                    .tipoGarrafaId(garrafa.getId())
                    .cantidad(det.getCantidad())
                    .precioUnitario(precioUnitario)
                    .build();

            pedido.agregarDetalle(detalle);
        }

        return pedidoRepository.save(pedido).getId();
    }
}