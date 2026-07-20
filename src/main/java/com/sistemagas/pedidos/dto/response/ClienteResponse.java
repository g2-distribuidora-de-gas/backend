package com.sistemagas.pedidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class ClienteResponse {
    private Long id;
    private String uuidOffline;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private String dni;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String placeId;
    private String geocodePrecision;
    private OffsetDateTime geoActualizadoEn;
    private Boolean activo;

    @Schema(description = "URL firmada (signed URL) de la foto de fachada. Null si el cliente no tiene foto.",
            example = "https://xxx.supabase.co/storage/v1/object/sign/...")
    private String urlFotoEvidencia;
}
