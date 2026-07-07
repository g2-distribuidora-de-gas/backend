package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoFotoResponse;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.Instant;

public interface PedidoService {

    PedidoResponse crear(PedidoRequest request);

    PedidoResponse obtenerPorUuidOffline(String uuidOffline);

    PedidoResponse obtenerPorId(Long id);

    List<PedidoResponse> listarTodos(Instant minUpdatedAt, Integer limit);

    void actualizarEstado(Long id, com.sistemagas.pedidos.enums.EstadoPedido estado);

    PedidoFotoResponse subirFoto(Long id, MultipartFile archivo, String descripcion);
}