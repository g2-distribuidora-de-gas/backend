package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DevolucionRequest {

    @NotNull(message = "El depósito destino es obligatorio")
    private Long depositoDestinoId;

    @NotNull(message = "El tipo de garrafa es obligatorio")
    private Long tipoGarrafaId;

    @NotNull(message = "El estado de garrafa es obligatorio")
    private Long estadoGarrafaId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    /** ID del pedido original. Opcional. */
    private Long pedidoId;

    @Size(max = 500)
    private String observaciones;
}
