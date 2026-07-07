package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoRuta;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RutaResponse {
    private Long id;
    private LocalDate fechaReparto;
    private Long repartidorId;
    private BigDecimal origenLat;
    private BigDecimal origenLng;
    private Integer distanciaTotalM;
    private Integer duracionTotalS;
    private String geometria;
    private EstadoRuta estado;
    private List<RutaPedidoResponse> paradas;
}
