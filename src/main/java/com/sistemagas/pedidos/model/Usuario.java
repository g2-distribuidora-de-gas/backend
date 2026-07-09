package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.RolUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Size(max = 30)
    @Column(length = 30)
    private String telefono;

    @Size(max = 300)
    @Column(length = 300)
    private String direccion;

    @Column(nullable = false)
    private boolean activo;

    @Override
    public Boolean getActivo() {
        return activo;
    }

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RolUsuario rol;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 150)
    @Column(unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Version
    private Long version;
}