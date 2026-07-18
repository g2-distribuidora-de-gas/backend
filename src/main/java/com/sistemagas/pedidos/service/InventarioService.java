package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.AjusteInventarioRequest;
import com.sistemagas.pedidos.dto.request.DevolucionRequest;
import com.sistemagas.pedidos.dto.request.ReparacionFinRequest;
import com.sistemagas.pedidos.dto.request.ReparacionInicioRequest;
import com.sistemagas.pedidos.dto.request.RoturaRequest;
import com.sistemagas.pedidos.dto.request.VentaStockRequest;
import com.sistemagas.pedidos.dto.response.MovimientoResponse;
import com.sistemagas.pedidos.model.Usuario;

import java.util.List;

public interface InventarioService {

    List<MovimientoResponse> registrarVenta(VentaStockRequest request, Usuario usuario);

    MovimientoResponse registrarDevolucion(DevolucionRequest request, Usuario usuario);

    MovimientoResponse registrarRotura(RoturaRequest request, Usuario usuario);

    MovimientoResponse iniciarReparacion(ReparacionInicioRequest request, Usuario usuario);

    MovimientoResponse finalizarReparacion(ReparacionFinRequest request, Usuario usuario);

    MovimientoResponse ajustarInventario(AjusteInventarioRequest request, Usuario usuario);
}
