package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReparacionInicioRequest {

    @NotNull(message = "El depósito origen es obligatorio")
    private Long depositoOrigenId;

    @NotNull(message = "El taller destino es obligatorio")
    private Long tallerDestinoId;

    @NotNull(message = "El tipo de garrafa es obligatorio")
    private Long tipoGarrafaId;

    @NotNull(message = "El estado origen es obligatorio")
    private Long estadoOrigenId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    @Size(max = 500)
    private String observaciones;
}
