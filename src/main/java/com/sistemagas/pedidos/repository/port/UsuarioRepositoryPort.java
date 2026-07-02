package com.sistemagas.pedidos.repository.port;

import com.sistemagas.pedidos.model.Usuario;

import java.util.Optional;

/**
 * Puerto (interfaz) que define las operaciones sobre Usuario que Dev 1 (pedidos)
 * necesita. Dev 2 (catalog) debe implementar este puerto con su UsuarioRepository real.
 */
public interface UsuarioRepositoryPort {

    Optional<Usuario> findById(Long id);
}