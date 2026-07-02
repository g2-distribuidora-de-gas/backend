package com.sistemagas.pedidos.repository.port.impl;

import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.repository.GarrafaRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GarrafaRepositoryAdapter implements GarrafaRepositoryPort {

    private final GarrafaRepository repo;

    @Override
    public Optional<GarrafaModel> findById(Long id) {
        return repo.findById(id).map(g -> g);
    }

    @Override
    public GarrafaModel save(GarrafaModel garrafa) {
        if (garrafa instanceof Garrafa) {
            return repo.save((Garrafa) garrafa);
        }
        throw new IllegalArgumentException("El modelo debe ser una instancia de la entidad Garrafa");
    }
}
