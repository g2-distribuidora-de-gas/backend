package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AjusteInventarioRequest {

    @NotNull(message = "El depósito es obligatorio")
    private Long depositoId;

    @NotNull(message = "El tipo de garrafa es obligatorio")
    private Long tipoGarrafaId;

    @NotNull(message = "El estado de garrafa es obligatorio")
    private Long estadoGarrafaId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @NotNull(message = "El tipo de ajuste es obligatorio")
    private TipoAjuste tipoAjuste;

    @NotBlank(message = "Las observaciones son obligatorias en ajustes de inventario")
    @Size(max = 1000)
    private String observaciones;

    public enum TipoAjuste {
        ENTRADA, SALIDA
    }
}
