package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;

import java.util.List;

public interface UsuarioService {
    List<UsuarioResponse> listarTodos();
    UsuarioResponse crear(UsuarioRequest request);
}
