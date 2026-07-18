package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.TipoGarrafaStockRequest;
import com.sistemagas.pedidos.dto.response.EstadoGarrafaResponse;
import com.sistemagas.pedidos.dto.response.TipoGarrafaStockResponse;

import java.util.List;

public interface TipoGarrafaStockService {

    TipoGarrafaStockResponse crear(TipoGarrafaStockRequest request);

    TipoGarrafaStockResponse actualizar(Long id, TipoGarrafaStockRequest request);

    TipoGarrafaStockResponse obtenerPorId(Long id);

    List<TipoGarrafaStockResponse> listarActivos();

    List<TipoGarrafaStockResponse> listarTodos();

    TipoGarrafaStockResponse cambiarEstado(Long id, boolean activo);

    // Estados (catálogo de solo lectura, no se crean desde la API)
    List<EstadoGarrafaResponse> listarEstados();
}
