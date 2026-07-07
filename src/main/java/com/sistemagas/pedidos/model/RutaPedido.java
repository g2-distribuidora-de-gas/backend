package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.EstadoEntrega;
import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "distancia_desde_anterior_m")
    private Integer distanciaDesdeAnteriorM;

    @Column(name = "duracion_desde_anterior_s")
    private Integer duracionDesdeAnteriorS;

    @Column(name = "hora_estimada_llegada")
    private OffsetDateTime horaEstimadaLlegada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_entrega", nullable = false, length = 30)
    private EstadoEntrega estadoEntrega;
}
