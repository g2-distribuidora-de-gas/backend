package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class StockDepositoResponse {

    private DepositoResponse deposito;
    private List<StockItemResponse> stock;
    private Instant consultadoEn;

    @Data
    @Builder
    public static class StockItemResponse {
        private String tipoGarrafa;
        private String estado;
        private Integer cantidad;
    }
}
