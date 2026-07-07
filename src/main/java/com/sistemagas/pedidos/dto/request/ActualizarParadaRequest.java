package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarParadaRequest {
    
    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoEntrega nuevoEstado;
}
