package com.sistemagas.pedidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta del endpoint DEPRECATED de subida de foto asociada a un pedido")
public class PedidoFotoResponse {

    @Schema(description = "ID del pedido asociado", example = "42")
    private Long pedidoId;

    @Schema(description = "URL firmada del archivo subido a Supabase Storage",
            example = "https://<supabase>/storage/v1/object/sign/...")
    private String urlFotoEvidencia;

    @Schema(description = "Nombre original del archivo", example = "fachada.jpg")
    private String nombreArchivo;

    @Schema(description = "Content-Type del archivo", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Tamano del archivo en bytes", example = "204800")
    private Long tamanioBytes;
}