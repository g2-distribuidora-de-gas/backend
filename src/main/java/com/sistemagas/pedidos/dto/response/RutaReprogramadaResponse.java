package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.EstadoRuta;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RutaReprogramadaResponse {
    private Long rutaId;
    private LocalDate fechaReparto;
    private EstadoRuta estado;
    private UsuarioResumenRepartidor repartidor;
    private Instant updatedAt;

    private Integer totalParadas;
    private Integer totalEntregadas;
    private Integer totalFallidas;
    private Integer totalPendientes;

    private List<ParadaFalloResponse> paradasFallidas;
}