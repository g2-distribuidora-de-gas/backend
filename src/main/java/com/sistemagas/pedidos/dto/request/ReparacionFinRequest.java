package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReparacionFinRequest {

    @NotNull(message = "El ID del taller es obligatorio")
    private Long tallerId;

    @NotNull(message = "El tipo de garrafa es obligatorio")
    private Long tipoGarrafaId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    /**
     * Estado final de la garrafa. Debe ser "LLENA" (reparada) o "FUERA_SERVICIO" (irreparable).
     */
    @NotNull(message = "El estado final es obligatorio")
    private Long estadoFinalId;

    @Size(max = 500)
    private String observaciones;
}
