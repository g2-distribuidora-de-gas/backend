package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.enums.TipoMovimiento;
import com.sistemagas.pedidos.exception.StockInsuficienteException;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.StockGarrafa;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.repository.MovimientoGarrafaRepository;
import com.sistemagas.pedidos.repository.StockGarrafaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceImplTest {

    @Mock
    private StockGarrafaRepository stockGarrafaRepository;

    @Mock
    private MovimientoGarrafaRepository movimientoGarrafaRepository;

    @InjectMocks
    private MovimientoStockServiceImpl movimientoStockService;

    private Deposito deposito;
    private TipoGarrafaStock tipo;
    private EstadoGarrafa estado;

    @BeforeEach
    void setUp() {
        deposito = Deposito.builder().id(1L).nombre("Central").build();
        tipo = TipoGarrafaStock.builder().id(1L).codigo("10KG").build();
        estado = EstadoGarrafa.builder().id(1L).codigo("LLENA").build();
    }

    @Test
    void decrementarStock_ExistenteYSuficiente_Success() {
        StockGarrafa stock = StockGarrafa.builder()
                .id(1L).deposito(deposito).tipoGarrafa(tipo).estadoGarrafa(estado).cantidad(50).build();

        when(stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado))
                .thenReturn(Optional.of(stock));

        movimientoStockService.decrementarStock(deposito, tipo, estado, 10);

        assertEquals(40, stock.getCantidad());
        verify(stockGarrafaRepository).save(stock);
    }

    @Test
    void decrementarStock_ExistentePeroInsuficiente_ThrowsException() {
        StockGarrafa stock = StockGarrafa.builder()
                .id(1L).deposito(deposito).tipoGarrafa(tipo).estadoGarrafa(estado).cantidad(5).build();

        when(stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado))
                .thenReturn(Optional.of(stock));

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class, () ->
                movimientoStockService.decrementarStock(deposito, tipo, estado, 10));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(stockGarrafaRepository, never()).save(any());
    }

    @Test
    void decrementarStock_NoExistente_ThrowsException() {
        when(stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado))
                .thenReturn(Optional.empty());

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class, () ->
                movimientoStockService.decrementarStock(deposito, tipo, estado, 10));

        assertTrue(ex.getMessage().contains("No existe registro de stock"));
        verify(stockGarrafaRepository, never()).save(any());
    }

    @Test
    void incrementarStock_Existente_IncrementaCantidad() {
        StockGarrafa stock = StockGarrafa.builder()
                .id(1L).deposito(deposito).tipoGarrafa(tipo).estadoGarrafa(estado).cantidad(20).build();

        when(stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado))
                .thenReturn(Optional.of(stock));

        movimientoStockService.incrementarStock(deposito, tipo, estado, 15);

        assertEquals(35, stock.getCantidad());
        verify(stockGarrafaRepository).save(stock);
    }

    @Test
    void incrementarStock_NoExistente_CreaNuevoRegistro() {
        when(stockGarrafaRepository.findByDepositoAndTipoGarrafaAndEstadoGarrafa(deposito, tipo, estado))
                .thenReturn(Optional.empty());

        movimientoStockService.incrementarStock(deposito, tipo, estado, 15);

        verify(stockGarrafaRepository).save(argThat(s ->
                s.getCantidad() == 15 &&
                s.getDeposito().equals(deposito) &&
                s.getTipoGarrafa().equals(tipo) &&
                s.getEstadoGarrafa().equals(estado)
        ));
    }

    @Test
    void registrarMovimiento_Success() {
        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .cantidad(10).build();

        when(movimientoGarrafaRepository.save(mov)).thenReturn(mov);

        MovimientoGarrafa guardado = movimientoStockService.registrarMovimiento(mov);

        assertNotNull(guardado);
        verify(movimientoGarrafaRepository).save(mov);
    }
}
