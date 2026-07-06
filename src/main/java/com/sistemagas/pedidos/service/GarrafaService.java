package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.GarrafaRequest;
import com.sistemagas.pedidos.dto.response.GarrafaResponse;

import java.util.List;

import java.time.Instant;

public interface GarrafaService {
    List<GarrafaResponse> listarTodas(Instant minUpdatedAt, Integer limit);
    GarrafaResponse crear(GarrafaRequest request);
    GarrafaResponse actualizar(Long id, GarrafaRequest request);
}
