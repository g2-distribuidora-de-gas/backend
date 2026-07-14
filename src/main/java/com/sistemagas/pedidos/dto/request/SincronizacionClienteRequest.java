package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Lote de clientes creados offline que el cliente envia para sincronizar contra el servidor")
public class SincronizacionClienteRequest {

    @NotEmpty(message = "La lista de clientes no puede estar vacia")
    @Valid
    @Schema(description = "Clientes a sincronizar. Cada cliente debe incluir su uuidOffline generado en la app")
    private List<ClienteRequest> clientes;
}
