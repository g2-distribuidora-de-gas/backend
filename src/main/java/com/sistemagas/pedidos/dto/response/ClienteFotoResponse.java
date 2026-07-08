package com.sistemagas.pedidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta para la foto de fachada de un cliente")
public class ClienteFotoResponse {

    @Schema(description = "ID del cliente", example = "7")
    private Long clienteId;

    @Schema(description = "Path del archivo en Supabase Storage (bucket privado)",
            example = "cliente-7/abc-uuid.jpg")
    private String objectPath;

    @Schema(description = "URL firmada (signed URL) generada on-the-fly para acceder al archivo",
            example = "https://xxx.supabase.co/storage/v1/object/sign/...")
    private String urlFotoEvidencia;

    @Schema(description = "Nombre original del archivo", example = "fachada.jpg")
    private String nombreArchivo;

    @Schema(description = "Content-Type del archivo", example = "image/jpeg")
    private String contentType;

    @Schema(description = "Tamano del archivo en bytes", example = "245678")
    private Long tamanioBytes;
}
