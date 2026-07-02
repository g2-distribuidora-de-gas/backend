package com.sistemagas.pedidos.util;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GarrafaStockHelper {

    private final GarrafaRepositoryPort garrafaRepositoryPort;

    public Map<Long, Garrafa> cargarYValidar(List<PedidoDetalleRequest> detalles) {
        Map<Long, Garrafa> mapa = new HashMap<>();
        for (PedidoDetalleRequest d : detalles) {
            Garrafa g = mapa.computeIfAbsent(d.getGarrafaId(), id ->
                    garrafaRepositoryPort.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    Constantes.MSG_GARRAFA_NO_ENCONTRADA + ": id=" + id)));
            if (g.getStockDisponible() < d.getCantidad()) {
                throw new BusinessException(
                        String.format("Stock insuficiente para garrafa id=%d. Disponible: %d, Solicitado: %d",
                                d.getGarrafaId(), g.getStockDisponible(), d.getCantidad()));
            }
        }
        return mapa;
    }
}