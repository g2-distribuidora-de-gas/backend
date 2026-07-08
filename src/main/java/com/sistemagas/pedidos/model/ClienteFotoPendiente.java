package com.sistemagas.pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cliente_foto_pendiente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteFotoPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "object_path", nullable = false, length = 500)
    private String objectPath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}
