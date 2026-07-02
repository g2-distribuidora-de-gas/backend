package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.UsuarioModel;
import com.sistemagas.pedidos.repository.port.UsuarioRepositoryPort;
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

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final GarrafaStockHelper garrafaStockHelper;
    private final SincronizacionPedidoSaver pedidoSaver;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SincronizacionResponse.Procesado procesar(PedidoRequest request) {
        UsuarioModel usuario = usuarioRepositoryPort.findById(request.getUsuarioId())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario no encontrado: id=" + request.getUsuarioId()));

        Map<Long, GarrafaModel> garrafas = garrafaStockHelper.cargarYValidar(request.getDetalles());

        Long pedidoId = pedidoSaver.guardar(request, usuario, garrafas);

        log.info("Pedido sincronizado: id={}, uuidOffline={}, detalles={}",
                pedidoId, request.getUuidOffline(), request.getDetalles().size());

        return SincronizacionResponse.Procesado.builder()
                .uuidOffline(request.getUuidOffline())
                .pedidoId(pedidoId)
                .build();
    }
}