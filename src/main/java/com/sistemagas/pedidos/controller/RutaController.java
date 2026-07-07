package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.request.RutaPlanificarRequest;
import com.sistemagas.pedidos.dto.response.ClienteResponse;
import com.sistemagas.pedidos.dto.response.RutaPedidoResponse;
import com.sistemagas.pedidos.dto.response.RutaResponse;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.service.RutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;

    @PostMapping("/planificar")
    public ResponseEntity<RutaResponse> planificarRuta(@Valid @RequestBody RutaPlanificarRequest request) {
        Ruta ruta = rutaService.planificarRuta(request.getRepartidorId(), request.getPedidosIds());
        return new ResponseEntity<>(mapToResponse(ruta), HttpStatus.CREATED);
    }

    @GetMapping("/mis-rutas/{repartidorId}")
    public ResponseEntity<RutaResponse> obtenerMiRutaActiva(@PathVariable Long repartidorId) {
        Ruta ruta = rutaService.obtenerRutaActivaRepartidor(repartidorId);
        return ResponseEntity.ok(mapToResponse(ruta));
    }

    @PatchMapping("/paradas/{rutaPedidoId}")
    public ResponseEntity<Void> actualizarEstadoParada(@PathVariable Long rutaPedidoId,
                                                       @Valid @RequestBody ActualizarParadaRequest request) {
        rutaService.actualizarEstadoParada(rutaPedidoId, request.getNuevoEstado());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{rutaId}/estado")
    public ResponseEntity<RutaResponse> cambiarEstadoRuta(@PathVariable Long rutaId,
                                                          @RequestParam EstadoRuta estado) {
        Ruta ruta = rutaService.cambiarEstadoRuta(rutaId, estado);
        return ResponseEntity.ok(mapToResponse(ruta));
    }

    private RutaResponse mapToResponse(Ruta ruta) {
        return RutaResponse.builder()
                .id(ruta.getId())
                .fechaReparto(ruta.getFechaReparto())
                .repartidorId(ruta.getRepartidor().getId())
                .origenLat(ruta.getOrigenLat())
                .origenLng(ruta.getOrigenLng())
                .distanciaTotalM(ruta.getDistanciaTotalM())
                .duracionTotalS(ruta.getDuracionTotalS())
                .geometria(ruta.getGeometria())
                .estado(ruta.getEstado())
                .paradas(ruta.getParadas().stream().map(this::mapRutaPedido).collect(Collectors.toList()))
                .build();
    }

    private RutaPedidoResponse mapRutaPedido(RutaPedido rp) {
        return RutaPedidoResponse.builder()
                .id(rp.getId())
                .pedidoId(rp.getPedido().getId())
                .orden(rp.getOrden())
                .distanciaDesdeAnteriorM(rp.getDistanciaDesdeAnteriorM())
                .duracionDesdeAnteriorS(rp.getDuracionDesdeAnteriorS())
                .estadoEntrega(rp.getEstadoEntrega())
                .cliente(mapCliente(rp.getPedido().getCliente()))
                .build();
    }

    private ClienteResponse mapCliente(Cliente cliente) {
        if (cliente == null) return null;
        return ClienteResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .latitud(cliente.getLatitud())
                .longitud(cliente.getLongitud())
                .build();
    }
}
