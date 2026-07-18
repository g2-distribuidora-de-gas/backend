package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.TipoDeposito;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepositoResponse {

    private Long id;
    private String nombre;
    private TipoDeposito tipo;
    private String descripcion;
    private boolean activo;
    private String vehiculoPatente;

    /** Solo presente cuando tipo = CAMION. */
    private RepartidorResumen repartidor;

    @Data
    @Builder
    public static class RepartidorResumen {
        private Long id;
        private String nombre;
        private String email;
    }
}
