package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.service.ClienteFotoService;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionPedidoProcessor {

    private final ClienteRepository clienteRepository;
    private final TipoGarrafaStockRepository tipoGarrafaStockRepository;
    private final SincronizacionPedidoSaver pedidoSaver;
    private final ClienteFotoService clienteFotoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SincronizacionResponse.Procesado procesar(PedidoRequest request, String emailAutenticado) {
        Cliente cliente = resolverCliente(request);

        java.util.Map<Long, TipoGarrafaStock> garrafas = new java.util.HashMap<>();
        for (PedidoDetalleRequest det : request.getDetalles()) {
            TipoGarrafaStock garrafa = tipoGarrafaStockRepository.findById(det.getTipoGarrafaId())
                    .orElseThrow(() -> new BusinessException("Garrafa no encontrada id: " + det.getTipoGarrafaId()));
            garrafas.put(garrafa.getId(), garrafa);
        }

        Long pedidoId = pedidoSaver.guardar(request, cliente, garrafas, emailAutenticado);

        try {
            clienteFotoService.asociarImagenACliente(cliente.getId());
        } catch (RuntimeException ex) {
            log.warn("Fallo asociando imagen pendiente al cliente {}. El pedido queda creado igual.",
                    cliente.getId(), ex);
        }

        log.info("Pedido sincronizado: id={}, uuidOffline={}, detalles={}",
                pedidoId, request.getUuidOffline(), request.getDetalles().size());

        return SincronizacionResponse.Procesado.builder()
                .uuidOffline(request.getUuidOffline())
                .pedidoId(pedidoId)
                .build();
    }

    private Cliente resolverCliente(PedidoRequest request) {
        String uuidOffline = request.getClienteUuidOffline();
        if (uuidOffline != null && !uuidOffline.isBlank()) {
            return clienteRepository.findByUuidOffline(uuidOffline)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cliente no encontrado por uuidOffline: " + uuidOffline));
        }
        Long clienteId = request.getClienteId();
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cliente no encontrado: id=" + clienteId));
    }
}