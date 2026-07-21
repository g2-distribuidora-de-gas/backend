package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request del administrador para agregar o editar notas de un recorrido")
public class ActualizarNotasAdminRequest {

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    @Schema(description = "Notas del administrador visibles para el repartidor. Null o vacio para borrar.",
            example = "Llevar cambio de 5000. El cliente del punto 3 pide que llegues antes de las 10.")
    private String notasAdmin;
}
