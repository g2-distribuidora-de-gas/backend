package com.sistemagas.pedidos.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estados_garrafa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoGarrafa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único del estado. Ej: "LLENA", "VACIA", "REPARACION".
     */
    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(length = 100)
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
