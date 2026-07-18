package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StockTotalResponse {

    private List<StockDepositoResponse.StockItemResponse> stock;
    private Integer depositosIncluidos;
    private Instant consultadoEn;
}
