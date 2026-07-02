package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoResponse;

public interface PedidoService {

    PedidoResponse crear(PedidoRequest request);

    PedidoResponse obtenerPorUuidOffline(String uuidOffline);
}