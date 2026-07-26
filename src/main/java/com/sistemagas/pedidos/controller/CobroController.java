package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.RegistrarCobroRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.PagoPedidoResponse;
import com.sistemagas.pedidos.dto.response.ResumenCobroResponse;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.service.CobroService;
import com.sistemagas.pedidos.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/cobros")
@RequiredArgsConstructor
@Tag(name = "Cobros", description = "Registro y consulta de cobros en punto de entrega")
public class CobroController {

    private final CobroService cobroService;
    private final AuthenticationHelper authenticationHelper;

    // ─── Registrar cobro ──────────────────────────────────────────────────────

    @PostMapping(path = "/paradas/{rutaPedidoId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
            summary = "Registrar o actualizar el cobro de una parada",
            description = """
                    Registra el cobro de una parada de entrega. El estadoPago se deriva automáticamente:
                    - efectivo + transferencia == total → PAGADO (inmutable tras registrarse)
                    - 0 < suma < total              → PARCIAL (actualizable)
                    - suma == 0                    → PENDIENTE (requiere motivoPendiente, actualizable)

                    Si la transferencia es mayor a 0, el comprobante es obligatorio.
                    Si ya existe un cobro PARCIAL o PENDIENTE, lo actualiza (upsert).
                    Un cobro PAGADO es inmutable.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Cobro registrado/actualizado",
                    content = @Content(schema = @Schema(implementation = PagoPedidoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Montos negativos, motivo faltante en PENDIENTE o comprobante faltante en transferencia"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Parada de otro repartidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "El cobro ya fue completado (PAGADO) y es inmutable"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422",
                    description = "Parada fallida / monto excede el total / parada no entregada para cobro completo")
    })
    public ResponseEntity<ApiResponse<PagoPedidoResponse>> registrarCobro(
            @Parameter(description = "ID de la parada (RutaPedido)", example = "10")
            @PathVariable Long rutaPedidoId,
            @Parameter(description = "Datos del cobro. En multipart, usar JSON String o application/json en el request part.")
            @Valid @RequestPart("request") RegistrarCobroRequest request,
            @Parameter(description = "Foto del comprobante (obligatorio si hay transferencia)")
            @RequestPart(value = "comprobante", required = false) org.springframework.web.multipart.MultipartFile comprobante) {

        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
        PagoPedidoResponse response = cobroService.registrarCobro(rutaPedidoId, request, comprobante, autenticado);

        return ResponseEntity.ok(ApiResponse.<PagoPedidoResponse>builder()
                .exito(true)
                .mensaje("Cobro registrado correctamente")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    // ─── Resumen de cobro ─────────────────────────────────────────────────────

    @GetMapping("/paradas/{rutaPedidoId}")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(
            summary = "Resumen de cobro de una parada",
            description = """
                    Devuelve el total a cobrar (calculado a partir de cantidadEntregada × precioUnitario),
                    el desglose por tipo de garrafa y el cobro ya registrado (si existe).
                    Un REPARTIDOR solo puede consultar sus propias paradas.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Resumen de cobro",
                    content = @Content(schema = @Schema(implementation = ResumenCobroResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Parada de otro repartidor"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Parada no encontrada")
    })
    public ResponseEntity<ApiResponse<ResumenCobroResponse>> obtenerResumenCobro(
            @Parameter(description = "ID de la parada (RutaPedido)", example = "10")
            @PathVariable Long rutaPedidoId) {

        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
        ResumenCobroResponse response = cobroService.obtenerResumenCobro(rutaPedidoId, autenticado);

        return ResponseEntity.ok(ApiResponse.<ResumenCobroResponse>builder()
                .exito(true)
                .mensaje("Resumen de cobro obtenido")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    // ─── Listados admin ───────────────────────────────────────────────────────

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(
            summary = "Listar cobros pendientes o parciales (admin)",
            description = "Devuelve todos los cobros cuyo estado es PENDIENTE o PARCIAL. " +
                    "Útil para el panel de cuentas corrientes del administrador.")
    public ResponseEntity<ApiResponse<List<PagoPedidoResponse>>> listarPendientes() {
        List<PagoPedidoResponse> response = cobroService.listarCobrosPendientes();
        return ResponseEntity.ok(ApiResponse.<List<PagoPedidoResponse>>builder()
                .exito(true)
                .mensaje("Cobros pendientes obtenidos")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/mis-cobros")
    @PreAuthorize("hasRole('REPARTIDOR')")
    @Operation(
            summary = "Mis cobros (repartidor)",
            description = "Devuelve todos los cobros del repartidor autenticado. " +
                    "Útil para la pantalla de cobros de la app del repartidor.")
    public ResponseEntity<ApiResponse<List<PagoPedidoResponse>>> listarMisCobros() {
        Usuario autenticado = authenticationHelper.getUsuarioAutenticado();
        List<PagoPedidoResponse> response = cobroService.listarMisCobros(autenticado);
        return ResponseEntity.ok(ApiResponse.<List<PagoPedidoResponse>>builder()
                .exito(true)
                .mensaje("Mis cobros obtenidos")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }
}
