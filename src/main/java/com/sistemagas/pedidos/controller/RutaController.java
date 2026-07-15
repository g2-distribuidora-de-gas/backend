package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.ActualizarParadaRequest;
import com.sistemagas.pedidos.dto.request.RutaPlanificarRequest;
import com.sistemagas.pedidos.dto.response.ClienteResponse;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.dto.response.RutaPedidoResponse;
import com.sistemagas.pedidos.dto.response.RutaReprogramadaResponse;
import com.sistemagas.pedidos.dto.response.RutaResponse;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.service.RutaService;
import com.sistemagas.pedidos.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
@Tag(name = "Rutas", description = "Planificacion y seguimiento de rutas de reparto")
public class RutaController {

    private final RutaService rutaService;
    private final AuthenticationHelper authenticationHelper;

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
        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
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
        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
        rutaService.actualizarEstadoParada(rutaPedidoId, request, autenticado);
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
        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
        Ruta ruta = rutaService.cambiarEstadoRuta(rutaId, estado, autenticado);
        return ResponseEntity.ok(mapToResponse(ruta));
    }

    @GetMapping("/paradas/{rutaPedidoId}/pedido")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Detalle de un pedido asociado a una parada (vista del repartidor)",
            description = "Retorna el pedido vinculado a una parada especifica en formato liviano " +
                    "optimizado para la app del repartidor (sin campos administrativos). " +
                    "Un REPARTIDOR solo puede consultar paradas de sus propias rutas.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pedido devuelto en formato DeliveryReadOnlyResponse",
                    content = @Content(schema = @Schema(implementation = DeliveryReadOnlyResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "La parada pertenece a una ruta de otro repartidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "La parada no existe")
    })
    public ResponseEntity<DeliveryReadOnlyResponse> obtenerPedidoDeParada(
            @Parameter(description = "ID de la parada (RutaPedido)", example = "10") @PathVariable Long rutaPedidoId) {
        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
        DeliveryReadOnlyResponse response = rutaService.obtenerPedidoDeParada(rutaPedidoId, autenticado);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reprogramadas")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reporte de rutas REPROGRAMADAS con detalle de fallos",
            description = "Lista las rutas en estado REPROGRAMADA con el detalle de cada parada fallida "
                    + "(motivo, cliente, pedido, cantidades). Útil para análisis operativo y "
                    + "planificación de reintentos.")
    public ResponseEntity<List<RutaReprogramadaResponse>> listarReprogramadas(
            @Parameter(description = "Fecha minima de reparto (inclusive). Default: hace 30 dias.",
                    example = "2026-06-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fecha maxima de reparto (inclusive). Default: hoy.",
                    example = "2026-07-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @Parameter(description = "Filtro opcional por repartidor dueño de la ruta", example = "5")
            @RequestParam(required = false) Long repartidorId,
            @Parameter(description = "Filtro opcional por updatedAt >= este instante (ISO-8601)",
                    example = "2026-07-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant minUpdatedAt,
            @Parameter(description = "Cantidad maxima de rutas a retornar (1-1000)", example = "100")
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(1000) Integer limit) {
        return ResponseEntity.ok(rutaService.listarReprogramadas(
                fechaDesde, fechaHasta, repartidorId, minUpdatedAt, limit));
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