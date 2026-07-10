package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta con el detalle completo de un pedido")
public class PedidoResponse {

    @Schema(description = "ID interno del pedido", example = "42")
    private Long id;

    @Schema(description = "UUID offline con el que se creo el pedido (null si fue creado online)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String uuidOffline;

    @Schema(description = "ID del cliente asociado", example = "7")
    private Long clienteId;

    @Schema(description = "Nombre del cliente asociado", example = "Juan Pérez")
    private String clienteNombre;

    @Schema(description = "ID del usuario que creo el pedido", example = "3")
    private Long creadorId;

    @Schema(description = "Nombre del usuario que creo el pedido", example = "Maria Preventista")
    private String creadorNombre;

    @Schema(description = "Direccion donde se entregara el pedido", example = "Av. Corrientes 1234, CABA")
    private String direccionEntrega;

    @Schema(description = "Estado actual del pedido", example = "PENDIENTE")
    private EstadoPedido estado;

    @Schema(description = "URL firmada de la foto de fachada del cliente (puede expirar)",
            example = "https://<supabase>/storage/v1/object/sign/...")
    private String urlFotoEvidencia;

    @Schema(description = "Monto total del pedido", example = "15000.00")
    private BigDecimal total;

    @Schema(description = "Fecha de creacion del pedido (UTC)", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "Fecha de ultima actualizacion del pedido (UTC)", example = "2026-01-15T11:00:00Z")
    private Instant updatedAt;

    @Builder.Default
    @Schema(description = "Lineas de garrafas que componen el pedido")
    private List<PedidoDetalleResponse> detalles = new ArrayList<>();
}