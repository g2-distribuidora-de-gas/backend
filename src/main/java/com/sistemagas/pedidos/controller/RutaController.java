package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.request.RutaPlanificarRequest;
import com.sistemagas.pedidos.dto.response.ClienteResponse;
import com.sistemagas.pedidos.dto.response.RutaPedidoResponse;
import com.sistemagas.pedidos.dto.response.RutaResponse;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.RutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
@Tag(name = "Rutas", description = "Planificacion y seguimiento de rutas de reparto")
public class RutaController {

    private final RutaService rutaService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/planificar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Planificar una nueva ruta de reparto",
            description = "Crea una ruta asignando un listado de pedidos a un repartidor. " +
                    "Calcula el orden optimo y la distancia/duracion total usando el proveedor de routing configurado.")
    public ResponseEntity<RutaResponse> planificarRuta(
            @Valid @RequestBody RutaPlanificarRequest request) {
        Ruta ruta = rutaService.planificarRuta(request.getRepartidorId(), request.getPedidosIds());
        return new ResponseEntity<>(mapToResponse(ruta), HttpStatus.CREATED);
    }

    @GetMapping("/mis-rutas/{repartidorId}")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Obtener la ruta activa de un repartidor",
            description = "Retorna la ruta actualmente en curso del repartidor con sus paradas. " +
                    "Un REPARTIDOR solo puede consultar sus propias rutas.")
    public ResponseEntity<RutaResponse> obtenerMiRutaActiva(
            @Parameter(description = "ID del repartidor", example = "5") @PathVariable Long repartidorId) {
        Usuario autenticado = getUsuarioAutenticado();
        Long uid = autenticado.getId();
        if (autenticado.getRol().name().equals("REPARTIDOR") && !uid.equals(repartidorId)) {
            throw new BusinessException("No puedes ver rutas de otro repartidor");
        }
        Ruta ruta = rutaService.obtenerRutaActivaRepartidor(repartidorId);
        return ResponseEntity.ok(mapToResponse(ruta));
    }

    @PatchMapping("/paradas/{rutaPedidoId}")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Actualizar el estado de una parada",
            description = "Marca el estado de entrega de una parada especifica (ej: ENTREGADO, NO_ENTREGADO). " +
                    "Un REPARTIDOR solo puede modificar paradas de sus propias rutas.")
    public ResponseEntity<Void> actualizarEstadoParada(
            @Parameter(description = "ID de la parada (RutaPedido)", example = "10") @PathVariable Long rutaPedidoId,
            @Valid @RequestBody ActualizarParadaRequest request) {
        Usuario autenticado = getUsuarioAutenticado();
        rutaService.actualizarEstadoParada(rutaPedidoId, request.getNuevoEstado(), autenticado);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{rutaId}/estado")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Cambiar el estado de una ruta",
            description = "Cambia el estado general de la ruta (ej: PLANIFICADA, EN_CURSO, COMPLETADA, CANCELADA). " +
                    "Un REPARTIDOR solo puede cambiar el estado de sus propias rutas.")
    public ResponseEntity<RutaResponse> cambiarEstadoRuta(
            @Parameter(description = "ID de la ruta", example = "1") @PathVariable Long rutaId,
            @Parameter(description = "Nuevo estado de la ruta", example = "EN_CURSO") @RequestParam EstadoRuta estado) {
        Usuario autenticado = getUsuarioAutenticado();
        Ruta ruta = rutaService.cambiarEstadoRuta(rutaId, estado, autenticado);
        return ResponseEntity.ok(mapToResponse(ruta));
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en la base de datos"));
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
