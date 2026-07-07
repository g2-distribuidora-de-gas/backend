package com.sistemagas.pedidos.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String placeId;
    private String precision;
}
