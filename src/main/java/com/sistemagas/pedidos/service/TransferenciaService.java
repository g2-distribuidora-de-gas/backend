package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.CargaCamionRequest;
import com.sistemagas.pedidos.dto.request.DescargaCamionRequest;
import com.sistemagas.pedidos.dto.request.TransferenciaRequest;
import com.sistemagas.pedidos.dto.response.MovimientoResponse;
import com.sistemagas.pedidos.model.Usuario;

import java.util.List;

public interface TransferenciaService {

    MovimientoResponse transferir(TransferenciaRequest request, Usuario usuario);

    List<MovimientoResponse> cargarCamion(CargaCamionRequest request, Usuario usuario);

    List<MovimientoResponse> descargarCamion(DescargaCamionRequest request, Usuario usuario);
}
