package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoResponse;

import java.util.List;

public interface PedidoService {

    PedidoResponse crear(PedidoRequest request);

    PedidoResponse obtenerPorUuidOffline(String uuidOffline);

    PedidoResponse obtenerPorId(Long id);

    List<PedidoResponse> listarTodos();

    void actualizarEstado(Long id, com.sistemagas.pedidos.enums.EstadoPedido estado);
}