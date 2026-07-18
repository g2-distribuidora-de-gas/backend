package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.response.DepositoResponse;
import com.sistemagas.pedidos.dto.response.StockDepositoResponse;
import com.sistemagas.pedidos.dto.response.StockTotalResponse;
import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.DepositoMapper;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.StockGarrafa;
import com.sistemagas.pedidos.repository.DepositoRepository;
import com.sistemagas.pedidos.repository.StockGarrafaRepository;
import com.sistemagas.pedidos.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockGarrafaRepository stockGarrafaRepository;
    private final DepositoRepository depositoRepository;
    private final DepositoMapper depositoMapper;

    @Override
    @Transactional(readOnly = true)
    public StockDepositoResponse getStockByDeposito(Long depositoId) {
        Deposito deposito = depositoRepository.findById(depositoId)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado"));

        List<StockGarrafa> stockList = stockGarrafaRepository.findByDepositoId(depositoId);
        return mapToStockDepositoResponse(deposito, stockList);
    }

    @Override
    @Transactional(readOnly = true)
    public StockDepositoResponse getStockCamion(Long camionId) {
        Deposito camion = depositoRepository.findById(camionId)
                .orElseThrow(() -> new ResourceNotFoundException("Camión no encontrado"));

        if (!TipoDeposito.CAMION.equals(camion.getTipo())) {
            throw new BusinessException("El ID proporcionado no pertenece a un camión");
        }

        List<StockGarrafa> stockList = stockGarrafaRepository.findByDepositoId(camionId);
        return mapToStockDepositoResponse(camion, stockList);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTotalResponse getStockTotal() {
        List<StockGarrafa> todoStock = stockGarrafaRepository.findAllByDepositoActivo();
        
        // Agrupar por tipo de garrafa y estado, y sumar cantidades
        Map<String, Integer> stockAgrupado = todoStock.stream()
                .collect(Collectors.groupingBy(
                        sg -> sg.getTipoGarrafa().getCodigo() + "|" + sg.getEstadoGarrafa().getCodigo(),
                        Collectors.summingInt(StockGarrafa::getCantidad)
                ));

        List<StockDepositoResponse.StockItemResponse> stockItems = new ArrayList<>();
        stockAgrupado.forEach((key, total) -> {
            String[] parts = key.split("\\|");
            stockItems.add(StockDepositoResponse.StockItemResponse.builder()
                    .tipoGarrafa(parts[0])
                    .estado(parts[1])
                    .cantidad(total)
                    .build());
        });

        // Contar depósitos únicos involucrados
        long countDepositos = todoStock.stream()
                .map(sg -> sg.getDeposito().getId())
                .distinct()
                .count();

        return StockTotalResponse.builder()
                .stock(stockItems)
                .depositosIncluidos((int) countDepositos)
                .consultadoEn(Instant.now())
                .build();
    }

    private StockDepositoResponse mapToStockDepositoResponse(Deposito deposito, List<StockGarrafa> stockList) {
        DepositoResponse depositoResponse = depositoMapper.toResponse(deposito);
        
        List<StockDepositoResponse.StockItemResponse> items = stockList.stream()
                .map(sg -> StockDepositoResponse.StockItemResponse.builder()
                        .tipoGarrafa(sg.getTipoGarrafa().getCodigo())
                        .estado(sg.getEstadoGarrafa().getCodigo())
                        .cantidad(sg.getCantidad())
                        .build())
                .collect(Collectors.toList());

        return StockDepositoResponse.builder()
                .deposito(depositoResponse)
                .stock(items)
                .consultadoEn(Instant.now())
                .build();
    }
}
