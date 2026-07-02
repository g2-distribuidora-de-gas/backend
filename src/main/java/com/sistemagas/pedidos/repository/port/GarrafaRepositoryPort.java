package com.sistemagas.pedidos.repository.port;

import com.sistemagas.pedidos.model.GarrafaModel;

import java.util.Optional;

/**
 * Puerto (interfaz) que define las operaciones sobre GarrafaModel que Dev 1 (pedidos)
 * necesita. Dev 2 (catalog) debe implementar este puerto con su GarrafaRepository real.
 */
public interface GarrafaRepositoryPort {

    Optional<GarrafaModel> findById(Long id);

    GarrafaModel save(GarrafaModel garrafa);
}