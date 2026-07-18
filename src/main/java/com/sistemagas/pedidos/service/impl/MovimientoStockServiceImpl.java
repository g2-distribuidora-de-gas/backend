package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.exception.StockInsuficienteException;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.StockGarrafa;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.repository.MovimientoGarrafaRepository;
import com.sistemagas.pedidos.repository.StockGarrafaRepository;
import com.sistemagas.pedidos.service.MovimientoStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoStockServiceImpl implements MovimientoStockService {

    private final StockGarrafaRepository stockGarrafaRepository;
    private final MovimientoGarrafaRepository movimientoGarrafaRepository;

    /**
     * Propagation.MANDATORY asegura que este método SIEMPRE sea llamado
     * dentro de una transacción ya existente (ej: TransferenciaService o InventarioService).
     * Nunca debe ejecutarse de forma aislada.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void decrementarStock(Deposito deposito, TipoGarrafaStock tipo, EstadoGarrafa estado, int cantidad) {
        StockGarrafa stock = stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado)
                .orElseThrow(() -> new StockInsuficienteException(
                        "No existe registro de stock en depósito [" + deposito.getNombre() + "] " +
                        "para tipo [" + tipo.getCodigo() + "] estado [" + estado.getCodigo() + "]"
                ));

        stock.decrementar(cantidad);
        stockGarrafaRepository.save(stock);
        log.debug("Stock decrementado: deposito={}, tipo={}, estado={}, delta={}, nuevoTotal={}",
                deposito.getId(), tipo.getCodigo(), estado.getCodigo(), cantidad, stock.getCantidad());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void incrementarStock(Deposito deposito, TipoGarrafaStock tipo, EstadoGarrafa estado, int cantidad) {
        StockGarrafa stock = stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado)
                .orElseGet(() -> StockGarrafa.builder()
                        .deposito(deposito)
                        .tipoGarrafa(tipo)
                        .estadoGarrafa(estado)
                        .cantidad(0)
                        .build());

        stock.incrementar(cantidad);
        stockGarrafaRepository.save(stock);
        log.debug("Stock incrementado: deposito={}, tipo={}, estado={}, delta={}, nuevoTotal={}",
                deposito.getId(), tipo.getCodigo(), estado.getCodigo(), cantidad, stock.getCantidad());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public MovimientoGarrafa registrarMovimiento(MovimientoGarrafa movimiento) {
        MovimientoGarrafa guardado = movimientoGarrafaRepository.save(movimiento);
        log.debug("Movimiento registrado: id={}, tipo={}", guardado.getId(), guardado.getTipoMovimiento());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MovimientoGarrafa> obtenerHistorial(
            Long depositoId, Long tipoId, com.sistemagas.pedidos.enums.TipoMovimiento tipoMov,
            java.time.Instant desde, java.time.Instant hasta, org.springframework.data.domain.Pageable pageable) {
        return movimientoGarrafaRepository.findByFiltros(depositoId, tipoId, tipoMov, desde, hasta, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<MovimientoGarrafa> obtenerMovimientosPedido(Long pedidoId) {
        return movimientoGarrafaRepository.findByPedidoIdOrderByFechaDesc(pedidoId);
    }
}
