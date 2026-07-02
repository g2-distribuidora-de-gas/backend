package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;

public interface SincronizacionService {

    SincronizacionResponse procesarPedidosOffline(SincronizacionRequest request);
}