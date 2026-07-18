package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.TipoGarrafaStock;

public interface MovimientoStockService {

    /**
     * Decrementa el stock. Valida que haya suficiente cantidad.
     */
    void decrementarStock(Deposito deposito, TipoGarrafaStock tipo, EstadoGarrafa estado, int cantidad);

    /**
     * Incrementa el stock. Si no existe la fila, la crea (upsert).
     */
    void incrementarStock(Deposito deposito, TipoGarrafaStock tipo, EstadoGarrafa estado, int cantidad);

    /**
     * Guarda el registro inmutable del movimiento en el historial.
     */
    MovimientoGarrafa registrarMovimiento(MovimientoGarrafa movimiento);

    /**
     * Historial paginado con filtros.
     */
    org.springframework.data.domain.Page<MovimientoGarrafa> obtenerHistorial(
            Long depositoId,
            Long tipoId,
            com.sistemagas.pedidos.enums.TipoMovimiento tipoMov,
            java.time.Instant desde,
            java.time.Instant hasta,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Movimientos por pedido.
     */
    java.util.List<MovimientoGarrafa> obtenerMovimientosPedido(Long pedidoId);
}
