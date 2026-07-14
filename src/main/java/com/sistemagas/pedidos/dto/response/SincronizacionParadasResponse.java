package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SincronizacionParadasResponse {
    private List<ParadaProcesada> procesados;
    private List<ParadaError> errores;

    @Data
    @Builder
    public static class ParadaProcesada {
        private String uuidOffline;
        private Long rutaPedidoId;
    }

    @Data
    @Builder
    public static class ParadaError {
        private String uuidOffline;
        private String error;
    }
}
