package com.sistemagas.pedidos.repository.port;

import com.sistemagas.pedidos.model.UsuarioModel;

import java.util.Optional;

/**
 * Puerto (interfaz) que define las operaciones sobre UsuarioModel que Dev 1 (pedidos)
 * necesita. Dev 2 (catalog) debe implementar este puerto con su UsuarioRepository real.
 */
public interface UsuarioRepositoryPort {

    Optional<UsuarioModel> findById(Long id);
}