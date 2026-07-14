package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionEstadoResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;

import java.util.List;

public interface SincronizacionService {

    SincronizacionResponse procesarPedidosOffline(SincronizacionRequest request, String emailAutenticado);

    SincronizacionEstadoResponse consultarEstado(List<String> uuids);
}