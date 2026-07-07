package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class ClienteResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String placeId;
    private OffsetDateTime geoActualizadoEn;
    private Boolean activo;
}
