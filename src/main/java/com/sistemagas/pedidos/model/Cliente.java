package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @Size(max = 30)
    @Column(length = 30)
    private String telefono;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String direccion;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Size(max = 100)
    @Column(name = "place_id", length = 100)
    private String placeId;

    @Size(max = 50)
    @Column(name = "geocode_precision", length = 50)
    private String geocodePrecision;

    @Column(name = "geo_actualizado_en")
    private OffsetDateTime geoActualizadoEn;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "foto_evidencia_path", length = 500)
    private String fotoEvidenciaPath;

    @Version
    private Long version;
}
