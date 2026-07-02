package com.sistemagas.pedidos.repository.port;

import com.sistemagas.pedidos.model.Garrafa;

import java.util.Optional;

/**
 * Puerto (interfaz) que define las operaciones sobre Garrafa que Dev 1 (pedidos)
 * necesita. Dev 2 (catalog) debe implementar este puerto con su GarrafaRepository real.
 */
public interface GarrafaRepositoryPort {

    Optional<Garrafa> findById(Long id);

    Garrafa save(Garrafa garrafa);
}