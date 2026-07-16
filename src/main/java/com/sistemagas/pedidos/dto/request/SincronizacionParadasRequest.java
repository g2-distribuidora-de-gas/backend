package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO para sincronizar un lote de paradas offline")
public class SincronizacionParadasRequest {

    @NotEmpty(message = "La lista de paradas no puede estar vacía")
    @Valid
    private List<SincronizacionParadaItemRequest> paradas;
}
