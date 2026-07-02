package com.sistemagas.pedidos.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SincronizacionResponse {

    private Integer total;
    private Instant servidorFecha;

    @Builder.Default
    private List<Procesado> procesados = new ArrayList<>();

    @Builder.Default
    private List<String> duplicados = new ArrayList<>();

    @Builder.Default
    private List<ErrorItem> errores = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Procesado {
        private String uuidOffline;
        private Long pedidoId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorItem {
        private String uuidOffline;
        private String motivo;
    }
}