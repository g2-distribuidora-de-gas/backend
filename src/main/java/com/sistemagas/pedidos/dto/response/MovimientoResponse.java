package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.TipoMovimiento;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MovimientoResponse {

    private Long id;
    private TipoMovimiento tipoMovimiento;
    private DepositoResumen depositoOrigen;
    private DepositoResumen depositoDestino;
    private TipoGarrafaStockResponse tipoGarrafa;
    private EstadoGarrafaResponse estadoOrigen;
    private EstadoGarrafaResponse estadoDestino;
    private Integer cantidad;
    private Long pedidoId;
    private UsuarioResumen usuario;
    private Instant fecha;
    private String observaciones;

    @Data
    @Builder
    public static class DepositoResumen {
        private Long id;
        private String nombre;
        private String tipo;
    }

    @Data
    @Builder
    public static class UsuarioResumen {
        private Long id;
        private String nombre;
        private String email;
    }
}
