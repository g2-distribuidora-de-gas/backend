package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rutas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruta extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_reparto", nullable = false)
    private LocalDate fechaReparto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id", nullable = false)
    private Usuario repartidor;

    @Column(name = "origen_lat", precision = 10, scale = 7)
    private BigDecimal origenLat;

    @Column(name = "origen_lng", precision = 10, scale = 7)
    private BigDecimal origenLng;

    @Column(name = "distancia_total_m")
    private Integer distanciaTotalM;

    @Column(name = "duracion_total_s")
    private Integer duracionTotalS;

    @Column(columnDefinition = "TEXT")
    private String geometria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoRuta estado;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RutaPedido> paradas = new ArrayList<>();

    public void agregarParada(RutaPedido parada) {
        paradas.add(parada);
        parada.setRuta(this);
    }
}
