package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SincronizacionRutasResponse {
    private List<RutaProcesada> procesados;
    private List<RutaError> errores;

    @Data
    @Builder
    public static class RutaProcesada {
        private String uuidOffline;
        private Long rutaId;
    }

    @Data
    @Builder
    public static class RutaError {
        private String uuidOffline;
        private Long rutaId;
        private String error;
    }
}