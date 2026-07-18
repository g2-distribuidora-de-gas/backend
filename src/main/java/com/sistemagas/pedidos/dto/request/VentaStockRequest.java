package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registra una venta desde un camión:
 * - Se decrementan garrafas LLENAS del camión.
 * - Se incrementan garrafas VACIAS del camión (las que recibe el repartidor del cliente).
 */
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaStockRequest {

    @NotNull(message = "El ID del camión es obligatorio")
    private Long camionId;

    @NotNull(message = "El tipo de garrafa es obligatorio")
    private Long tipoGarrafaId;

    @NotNull(message = "La cantidad entregada es obligatoria")
    @Min(value = 1, message = "La cantidad entregada debe ser mayor a 0")
    private Integer cantidadEntregadas;

    @NotNull(message = "La cantidad recibida es obligatoria")
    @Min(value = 0, message = "La cantidad recibida no puede ser negativa")
    private Integer cantidadRecibidas;

    /** ID del pedido asociado. Opcional. */
    private Long pedidoId;

    @Size(max = 500)
    private String observaciones;
}
