package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.response.StockDepositoResponse;
import com.sistemagas.pedidos.dto.response.StockTotalResponse;

public interface StockService {

    StockDepositoResponse getStockByDeposito(Long depositoId);

    StockDepositoResponse getStockCamion(Long camionId);

    StockTotalResponse getStockTotal();
}
