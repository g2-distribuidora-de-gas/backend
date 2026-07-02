package com.sistemagas.pedidos.repository.port.impl;

import com.sistemagas.pedidos.model.UsuarioModel;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.port.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioRepository repo;

    @Override
    public Optional<UsuarioModel> findById(Long id) {
        return repo.findById(id).map(u -> (UsuarioModel) u);
    }
}
