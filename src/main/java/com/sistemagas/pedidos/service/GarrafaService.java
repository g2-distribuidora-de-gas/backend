package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.GarrafaRequest;
import com.sistemagas.pedidos.dto.response.GarrafaResponse;

import java.util.List;

public interface GarrafaService {
    List<GarrafaResponse> listarTodas();
    GarrafaResponse crear(GarrafaRequest request);
}
