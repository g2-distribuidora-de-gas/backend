package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "DTO para sincronizar un lote de cambios de estado de rutas offline")
public class SincronizacionRutasRequest {

    @NotEmpty(message = "La lista de cambios no puede estar vacia")
    @Valid
    private List<SincronizacionRutaItemRequest> cambios;
}