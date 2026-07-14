package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ruta_pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La ruta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    @Column(nullable = false)
    private Integer orden;

    @Column(name = "distancia_desde_anterior_m")
    private Integer distanciaDesdeAnteriorM;

    @Column(name = "duracion_desde_anterior_s")
    private Integer duracionDesdeAnteriorS;

    @Column(name = "hora_estimada_llegada")
    private OffsetDateTime horaEstimadaLlegada;

    @NotNull(message = "El estado de entrega es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 30)
    private EstadoEntrega estadoEntrega;

    @Column(name = "motivo_fallo")
    private String motivoFallo;

    @Version
    private Long version;
}
