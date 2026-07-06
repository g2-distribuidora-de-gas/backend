package com.sistemagas.pedidos.util;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.GarrafaModel;
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

    public Map<Long, GarrafaModel> cargarYValidar(List<PedidoDetalleRequest> detalles) {
        Map<Long, GarrafaModel> mapa = new HashMap<>();
        for (PedidoDetalleRequest d : detalles) {
            GarrafaModel g = mapa.computeIfAbsent(d.getGarrafaId(), id ->
                    garrafaRepositoryPort.findByIdForUpdate(id)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    Constantes.MSG_GARRAFA_NO_ENCONTRADA + ": id=" + id)));
            Integer stockDisponible = g.getStockDisponible();
            Integer cantidad = d.getCantidad();
            if (stockDisponible == null || cantidad == null || stockDisponible < cantidad) {
                throw new BusinessException(
                        String.format("Stock insuficiente para garrafa id=%d. Disponible: %d, Solicitado: %d",
                                d.getGarrafaId(),
                                stockDisponible == null ? 0 : stockDisponible,
                                cantidad == null ? 0 : cantidad));
            }
        }
        return mapa;
    }

    public void validarYDescontar(GarrafaModel garrafa, Integer cantidad) {
        if (garrafa == null) {
            throw new BusinessException("No se puede descontar stock de una garrafa nula");
        }
        Integer stockDisponible = garrafa.getStockDisponible();
        Integer stockResultante = (stockDisponible != null && cantidad != null)
                ? stockDisponible - cantidad
                : null;

        if (stockDisponible == null || cantidad == null
                || stockDisponible < 0
                || stockResultante == null
                || stockResultante < 0) {
            throw new BusinessException(
                    String.format("No se puede descontar stock: el stock disponible (%s) es inferior a 0 o insuficiente para la cantidad solicitada (%s)",
                            stockDisponible == null ? "null" : stockDisponible,
                            cantidad == null ? "null" : cantidad));
        }

        garrafa.setStockDisponible(stockResultante);
    }
}