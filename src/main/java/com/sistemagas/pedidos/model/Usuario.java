package com.sistemagas.pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import com.sistemagas.pedidos.model.base.Auditable;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends Auditable implements UsuarioModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(length = 30)
    private String telefono;

    @Column(length = 300)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo;
}