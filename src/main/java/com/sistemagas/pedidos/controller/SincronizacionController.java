package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionEstadoResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionEstadoResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.dto.request.SincronizacionParadasRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionParadasResponse;
import com.sistemagas.pedidos.service.ClienteFotoService;
import com.sistemagas.pedidos.service.SincronizacionService;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(Constantes.API_SINCRONIZAR)
@RequiredArgsConstructor
@Tag(name = "Sincronizacion", description = "Sincronizacion de pedidos creados offline")
public class SincronizacionController {

    private final SincronizacionService sincronizacionService;
    private final ClienteFotoService clienteFotoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Sincronizar lote de pedidos creados offline",
            description = "Recibe pedidos guardados en IndexedDB del cliente y los procesa. " +
                    "Devuelve 3 listas: procesados, duplicados y errores.")
    public ResponseEntity<ApiResponse<SincronizacionResponse>> sincronizar(
            @Valid @RequestBody SincronizacionRequest request, org.springframework.security.core.Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        SincronizacionResponse response = sincronizacionService.procesarPedidosOffline(request, email);
        return ResponseEntity.ok(ApiResponse.ok(response, Constantes.MSG_SINCRONIZACION_OK));
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Consultar el estado de sincronizacion de una lista de UUIDs",
            description = "Recibe una lista de UUIDs offline y devuelve cuales ya fueron procesados " +
                    "en el servidor y cuales aun no. Util para que el cliente consulte antes de reenviar.")
    public ResponseEntity<ApiResponse<SincronizacionEstadoResponse>> consultarEstado(
            @Parameter(description = "Lista de UUIDs offline a consultar (separados por coma)",
                    example = "uuid-1,uuid-2,uuid-3")
            @RequestParam("uuids") List<String> uuids) {
        return ResponseEntity.ok(ApiResponse.ok(sincronizacionService.consultarEstado(uuids)));
    }

    @PostMapping("/clientes")
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Sincronizar lote de clientes creados offline",
            description = "Recibe clientes guardados en IndexedDB del dispositivo y los procesa.")
    public ResponseEntity<ApiResponse<com.sistemagas.pedidos.dto.response.SincronizacionClienteResponse>> sincronizarClientes(
            @Valid @RequestBody com.sistemagas.pedidos.dto.request.SincronizacionClienteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sincronizacionService.procesarClientesOffline(request), Constantes.MSG_SINCRONIZACION_OK));
    }

    @GetMapping("/clientes/estado")
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Consultar el estado de sincronizacion de clientes (por UUIDs)",
            description = "Devuelve cuales clientes ya fueron procesados en el servidor.")
    public ResponseEntity<ApiResponse<SincronizacionEstadoResponse>> consultarEstadoClientes(
            @Parameter(description = "Lista de UUIDs offline a consultar (separados por coma)")
            @RequestParam("uuids") List<String> uuids) {
        return ResponseEntity.ok(ApiResponse.ok(sincronizacionService.consultarEstadoClientes(uuids)));
    }

    @PostMapping("/paradas")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Sincronizar lote de estados de paradas offline",
            description = "Recibe paradas (entregadas/fallidas) guardadas en el dispositivo y las procesa.")
    public ResponseEntity<ApiResponse<SincronizacionParadasResponse>> sincronizarParadas(
            @Valid @RequestBody SincronizacionParadasRequest request, org.springframework.security.core.Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.ok(sincronizacionService.procesarParadasOffline(request, email), Constantes.MSG_SINCRONIZACION_OK));
    }

    @PostMapping(path = "/clientes/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Subir imagen pendiente de un cliente (flujo offline)",
            description = "Recibe la foto de fachada de un cliente como multipart. "
                    + "La imagen queda en cliente-pending/{clienteId}/... y se asocia automaticamente "
                    + "cuando un pedido de ese cliente se sincroniza via POST /api/sincronizacion. "
                    + "Retorna 409 Conflict si el cliente ya tiene foto o una imagen pendiente.")
    public ResponseEntity<ApiResponse<ImagenPendienteResponse>> subirImagenCliente(
            @Parameter(description = "ID del cliente al que pertenece la imagen", example = "1")
            @RequestParam("clienteId") Long clienteId,
            @Parameter(description = "Archivo de imagen (jpg/png/webp). En el form-data el campo debe llamarse 'archivo'.")
            @RequestParam("archivo") MultipartFile archivo,
            @Parameter(description = "Descripcion opcional de la evidencia (texto libre, no se persiste en Storage)")
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        String objectPath = clienteFotoService.subirImagenPendiente(clienteId, archivo, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new ImagenPendienteResponse(clienteId, objectPath),
                        "Imagen pendiente subida. Se asociara al sincronizar un pedido del cliente."));
    }

    public record ImagenPendienteResponse(Long clienteId, String objectPath) {}
}