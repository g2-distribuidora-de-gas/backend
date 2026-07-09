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
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.RutaService;
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
public class RutaController {

    private final RutaService rutaService;
    private final UsuarioRepository usuarioRepository;
    private final RutaRepository rutaRepository;
    private final RutaPedidoRepository rutaPedidoRepository;

    @PostMapping("/planificar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RutaResponse> planificarRuta(@Valid @RequestBody RutaPlanificarRequest request) {
        Ruta ruta = rutaService.planificarRuta(request.getRepartidorId(), request.getPedidosIds());
        return new ResponseEntity<>(mapToResponse(ruta), HttpStatus.CREATED);
    }

    @GetMapping("/mis-rutas/{repartidorId}")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RutaResponse> obtenerMiRutaActiva(@PathVariable Long repartidorId) {
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
    public ResponseEntity<Void> actualizarEstadoParada(@PathVariable Long rutaPedidoId,
                                                       @Valid @RequestBody ActualizarParadaRequest request) {
        Usuario autenticado = getUsuarioAutenticado();
        if (autenticado.getRol().name().equals("REPARTIDOR")) {
            RutaPedido parada = rutaPedidoRepository.findById(rutaPedidoId)
                    .orElseThrow(() -> new BusinessException("Parada no encontrada"));
            Long duenoId = parada.getRuta().getRepartidor().getId();
            if (!duenoId.equals(autenticado.getId())) {
                throw new BusinessException("No puedes modificar paradas de otro repartidor");
            }
        }
        rutaService.actualizarEstadoParada(rutaPedidoId, request.getNuevoEstado());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{rutaId}/estado")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<RutaResponse> cambiarEstadoRuta(@PathVariable Long rutaId,
                                                          @RequestParam EstadoRuta estado) {
        Usuario autenticado = getUsuarioAutenticado();
        if (autenticado.getRol().name().equals("REPARTIDOR")) {
            Ruta ruta = rutaRepository.findById(rutaId)
                    .orElseThrow(() -> new BusinessException("Ruta no encontrada"));
            if (!ruta.getRepartidor().getId().equals(autenticado.getId())) {
                throw new BusinessException("No puedes cambiar el estado de una ruta de otro repartidor");
            }
        }
        Ruta ruta = rutaService.cambiarEstadoRuta(rutaId, estado);
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
