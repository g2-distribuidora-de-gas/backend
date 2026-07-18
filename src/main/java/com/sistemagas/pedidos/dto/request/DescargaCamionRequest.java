package com.sistemagas.pedidos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DescargaCamionRequest {

    @NotNull(message = "El ID del camión es obligatorio")
    private Long camionId;

    @NotNull(message = "El ID del depósito central es obligatorio")
    private Long depositoCentralId;

    @NotNull(message = "Los ítems de descarga son obligatorios")
    @Valid
    private List<ItemDescarga> items;

    @Size(max = 500)
    private String observaciones;

    @Data
    public static class ItemDescarga {
        @NotNull(message = "El tipo de garrafa es obligatorio")
        private Long tipoGarrafaId;

        @NotNull(message = "El estado de garrafa es obligatorio")
        private Long estadoGarrafaId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a 0")
        private Integer cantidad;
    }
}
