package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.RegistrarCobroRequest;
import com.sistemagas.pedidos.dto.response.PagoPedidoResponse;
import com.sistemagas.pedidos.dto.response.ResumenCobroResponse;
import com.sistemagas.pedidos.model.Usuario;

import java.util.List;

public interface CobroService {

    /**
     * Registra o actualiza el cobro de una parada.
     *
     * <p>Reglas:
     * <ul>
     *   <li>Si ya existe un cobro {@code PAGADO} lanza {@code BusinessException} (inmutable).</li>
     *   <li>Si ya existe {@code PARCIAL} o {@code PENDIENTE}, lo actualiza (upsert).</li>
     *   <li>La parada debe ser {@code ENTREGADO} para registrar {@code PAGADO}.</li>
     *   <li>Paradas {@code FALLIDO} no aceptan cobro.</li>
     *   <li>El {@code EstadoPago} se deriva de los montos; el cliente no lo envía.</li>
     * </ul>
     */
    PagoPedidoResponse registrarCobro(Long rutaPedidoId, RegistrarCobroRequest request, org.springframework.web.multipart.MultipartFile comprobante, Usuario cobrador);

    /**
     * Devuelve el resumen de cobro de una parada:
     * total calculado, desglose por ítem y cobro ya existente (si hay).
     */
    ResumenCobroResponse obtenerResumenCobro(Long rutaPedidoId, Usuario autenticado);

    /** Cobros en estado {@code PARCIAL} o {@code PENDIENTE} — vista del admin. */
    List<PagoPedidoResponse> listarCobrosPendientes();

    /** Todos los cobros del repartidor autenticado. */
    List<PagoPedidoResponse> listarMisCobros(Usuario repartidor);
}
