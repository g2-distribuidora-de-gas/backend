package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.RolUsuario;

/**
 * Interfaz del modelo Usuario. Dev 1 (pedidos) la define con los metodos que necesita.
 * Dev 2 (catalog) la implementa con su entidad JPA real.
 */
public interface UsuarioModel {

    Long getId();

    String getNombre();

    String getApellido();

    String getEmail();

    RolUsuario getRol();

    Boolean getActivo();
}