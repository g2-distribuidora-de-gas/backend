package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.AjusteInventarioRequest;
import com.sistemagas.pedidos.dto.request.CargaCamionRequest;
import com.sistemagas.pedidos.dto.request.DescargaCamionRequest;
import com.sistemagas.pedidos.dto.request.DevolucionRequest;
import com.sistemagas.pedidos.dto.request.ReparacionFinRequest;
import com.sistemagas.pedidos.dto.request.ReparacionInicioRequest;
import com.sistemagas.pedidos.dto.request.RoturaRequest;
import com.sistemagas.pedidos.dto.request.TransferenciaRequest;
import com.sistemagas.pedidos.dto.request.VentaStockRequest;
import com.sistemagas.pedidos.dto.response.MovimientoPageResponse;
import com.sistemagas.pedidos.dto.response.MovimientoResponse;
import com.sistemagas.pedidos.enums.TipoMovimiento;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.MovimientoMapper;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.InventarioService;
import com.sistemagas.pedidos.service.MovimientoStockService;
import com.sistemagas.pedidos.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock/movimientos")
@RequiredArgsConstructor
@Tag(name = "Movimientos de Stock", description = "Operaciones de registro de movimientos (transferencias, cargas, ventas, ajustes) y consulta del historial")
public class MovimientoController {

    private final TransferenciaService transferenciaService;
    private final InventarioService inventarioService;
    private final MovimientoStockService movimientoStockService;
    private final MovimientoMapper movimientoMapper;
    private final UsuarioRepository usuarioRepository;

    private Usuario getUsuario(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    // ----------------------------------------------------------------
    // Historial y Consultas
    // ----------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Consultar historial de movimientos (Paginado)")
    public ResponseEntity<MovimientoPageResponse> listarHistorial(
            @RequestParam(required = false) Long depositoId,
            @RequestParam(required = false) Long tipoGarrafaId,
            @RequestParam(required = false) TipoMovimiento tipoMovimiento,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<MovimientoGarrafa> pagina = movimientoStockService.obtenerHistorial(
                depositoId, tipoGarrafaId, tipoMovimiento, desde, hasta, PageRequest.of(page, size));

        List<MovimientoResponse> content = pagina.getContent().stream()
                .map(movimientoMapper::toResponse)
                .collect(Collectors.toList());

        MovimientoPageResponse response = MovimientoPageResponse.builder()
                .content(content)
                .page(pagina.getNumber())
                .size(pagina.getSize())
                .totalElements(pagina.getTotalElements())
                .totalPages(pagina.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Obtener movimientos asociados a un pedido")
    public ResponseEntity<List<MovimientoResponse>> obtenerMovimientosPorPedido(@PathVariable Long pedidoId) {
        List<MovimientoGarrafa> movimientos = movimientoStockService.obtenerMovimientosPedido(pedidoId);
        return ResponseEntity.ok(movimientos.stream().map(movimientoMapper::toResponse).collect(Collectors.toList()));
    }

    // ----------------------------------------------------------------
    // Transferencias y Logística
    // ----------------------------------------------------------------

    @PostMapping("/transferir")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Transferencia simple entre depósitos")
    public ResponseEntity<MovimientoResponse> transferir(
            @Valid @RequestBody TransferenciaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(transferenciaService.transferir(request, getUsuario(authentication)));
    }

    @PostMapping("/carga-camion")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'PREVENTISTA')")
    @Operation(summary = "Cargar camión", description = "Traspaso de garrafas LLENAS desde un depósito hacia un camión")
    public ResponseEntity<List<MovimientoResponse>> cargarCamion(
            @Valid @RequestBody CargaCamionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(transferenciaService.cargarCamion(request, getUsuario(authentication)));
    }

    @PostMapping("/descarga-camion")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'PREVENTISTA')")
    @Operation(summary = "Descargar camión", description = "Traspaso de garrafas (en múltiples estados) desde el camión al depósito")
    public ResponseEntity<List<MovimientoResponse>> descargarCamion(
            @Valid @RequestBody DescargaCamionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(transferenciaService.descargarCamion(request, getUsuario(authentication)));
    }

    // ----------------------------------------------------------------
    // Ventas y Operaciones
    // ----------------------------------------------------------------

    @PostMapping("/venta")
    @Operation(summary = "Registrar venta desde camión", description = "Descuenta llenas y (opcionalmente) suma vacías. Idealmente se dispara automáticamente al entregar un pedido.")
    public ResponseEntity<List<MovimientoResponse>> registrarVenta(
            @Valid @RequestBody VentaStockRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventarioService.registrarVenta(request, getUsuario(authentication)));
    }

    @PostMapping("/devolucion")
    @Operation(summary = "Devolución de cliente", description = "Ingresa stock al sistema proveniente de un cliente (fuera del sistema)")
    public ResponseEntity<MovimientoResponse> registrarDevolucion(
            @Valid @RequestBody DevolucionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventarioService.registrarDevolucion(request, getUsuario(authentication)));
    }

    @PostMapping("/rotura")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reportar rotura", description = "Cambia el estado de una garrafa a FUERA_SERVICIO")
    public ResponseEntity<MovimientoResponse> registrarRotura(
            @Valid @RequestBody RoturaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventarioService.registrarRotura(request, getUsuario(authentication)));
    }

    @PostMapping("/reparacion/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Enviar a reparación", description = "Mueve una garrafa a un taller y le asigna el estado REPARACION")
    public ResponseEntity<MovimientoResponse> iniciarReparacion(
            @Valid @RequestBody ReparacionInicioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventarioService.iniciarReparacion(request, getUsuario(authentication)));
    }

    @PostMapping("/reparacion/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Retornar de reparación", description = "Recibe garrafa del taller en estado final (LLENA o FUERA_SERVICIO)")
    public ResponseEntity<MovimientoResponse> finalizarReparacion(
            @Valid @RequestBody ReparacionFinRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventarioService.finalizarReparacion(request, getUsuario(authentication)));
    }

    @PostMapping("/ajuste")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Ajuste manual de inventario", description = "Fuerza la adición o sustracción de stock sin contraparte operativa")
    public ResponseEntity<MovimientoResponse> ajustarInventario(
            @Valid @RequestBody AjusteInventarioRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(inventarioService.ajustarInventario(request, getUsuario(authentication)));
    }
}
