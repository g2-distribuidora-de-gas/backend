package com.sistemagas.pedidos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoRequest {

    @Size(max = 100)
    private String uuidOffline;

    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @NotBlank(message = "La direccion de entrega es obligatoria")
    @Size(max = 300)
    private String direccionEntrega;

    @NotEmpty(message = "El pedido debe tener al menos un detalle")
    @Valid
    private List<PedidoDetalleRequest> detalles;
}