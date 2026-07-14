package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.service.ClienteFotoService;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
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
    private final GarrafaStockHelper garrafaStockHelper;
    private final SincronizacionPedidoSaver pedidoSaver;
    private final ClienteFotoService clienteFotoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SincronizacionResponse.Procesado procesar(PedidoRequest request, String emailAutenticado) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cliente no encontrado: id=" + request.getClienteId()));

        Map<Long, GarrafaModel> garrafas = garrafaStockHelper.cargarYValidar(request.getDetalles());

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
}