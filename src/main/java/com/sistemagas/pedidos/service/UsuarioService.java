package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;

import java.util.List;

import java.time.Instant;

public interface UsuarioService {
    List<UsuarioResponse> listarTodos(Instant minUpdatedAt, Integer limit);
    UsuarioResponse crear(UsuarioRequest request);
    void eliminar(Long id);
    void reactivar(Long id);
}
