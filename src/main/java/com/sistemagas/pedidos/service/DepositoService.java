package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.DepositoRequest;
import com.sistemagas.pedidos.dto.response.DepositoResponse;
import com.sistemagas.pedidos.enums.TipoDeposito;

import java.util.List;

public interface DepositoService {

    DepositoResponse crear(DepositoRequest request);

    DepositoResponse actualizar(Long id, DepositoRequest request);

    DepositoResponse obtenerPorId(Long id);

    List<DepositoResponse> listarTodos();

    List<DepositoResponse> listarActivos();

    List<DepositoResponse> listarPorTipo(TipoDeposito tipo);

    DepositoResponse cambiarEstado(Long id, boolean activo);
}
