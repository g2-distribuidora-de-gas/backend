package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 300)
    private String direccion;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "place_id", length = 100)
    private String placeId;

    @Column(name = "geocode_precision", length = 50)
    private String geocodePrecision;

    @Column(name = "geo_actualizado_en")
    private OffsetDateTime geoActualizadoEn;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "foto_evidencia_path", length = 500)
    private String fotoEvidenciaPath;
}
