package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RutaPlanificarRequest {
    
    @NotNull(message = "El ID del repartidor es obligatorio")
    private Long repartidorId;

    @NotEmpty(message = "Debe enviar al menos un pedido para planificar")
    private List<Long> pedidosIds;
}
