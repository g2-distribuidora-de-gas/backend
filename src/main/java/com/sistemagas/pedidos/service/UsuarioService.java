package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;
import com.sistemagas.pedidos.model.Usuario;

import java.util.List;

import java.time.Instant;

public interface UsuarioService {
    List<UsuarioResponse> listarTodos(Instant minUpdatedAt, Integer limit);
    UsuarioResponse crear(UsuarioRequest request);
    void eliminar(Long id, Usuario solicitante);
    void reactivar(Long id, Usuario solicitante);
}

