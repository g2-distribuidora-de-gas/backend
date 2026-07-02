package com.sistemagas.pedidos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SincronizacionRequest {

    @NotEmpty(message = "La lista de pedidos no puede estar vacia")
    @Valid
    private List<PedidoRequest> pedidos;
}