package com.sistemagas.pedidos.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "DTO para la creacion de un pedido de garrafas")
public class PedidoRequest {

    @Size(max = 100)
    @Schema(description = "UUID generado en el cliente al crear el pedido offline (opcional, permite idempotencia)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String uuidOffline;

    @NotNull(message = "El ID del cliente es obligatorio")
    @Schema(description = "ID del cliente al que se asocia el pedido", example = "7")
    private Long clienteId;

    @Schema(description = "ID del usuario creador del pedido (se completa automaticamente con el usuario autenticado si se omite)",
            example = "3")
    private Long creadorId;

    @NotBlank(message = "La direccion de entrega es obligatoria")
    @Size(max = 300)
    @Schema(description = "Direccion exacta donde se entregara el pedido",
            example = "Av. Corrientes 1234, CABA")
    private String direccionEntrega;

    @Size(max = 1000)
    @Schema(description = "URL firmada de la foto de fachada del cliente (opcional, se resuelve desde el cliente asociado)",
            example = "https://<supabase>/storage/v1/object/sign/...")
    private String urlFotoEvidencia;

    @NotEmpty(message = "El pedido debe tener al menos un detalle")
    @Valid
    @Schema(description = "Lineas de garrafas que componen el pedido")
    private List<PedidoDetalleRequest> detalles;
}