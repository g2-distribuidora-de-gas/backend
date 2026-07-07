package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.PedidoFotoResponse;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.service.PedidoService;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;

@RestController
@RequestMapping(Constantes.API_PEDIDOS)
@RequiredArgsConstructor
@Validated
@Tag(name = "Pedidos", description = "Crear y consultar pedidos de garrafas")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(summary = "Crear un pedido individual (camino online)")
    public ResponseEntity<ApiResponse<PedidoResponse>> crear(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse response = pedidoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Pedido creado"));
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos",
            description = "Retorna la lista completa de pedidos registrados")
    public ResponseEntity<ApiResponse<List<PedidoResponse>>> listarTodos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant minUpdatedAt,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.listarTodos(minUpdatedAt, limit)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pedido por su ID",
            description = "Retorna el detalle completo del pedido, incluyendo lineas, " +
                    "nombre del usuario y tipo de garrafa")
    public ResponseEntity<ApiResponse<PedidoResponse>> obtenerPorId(
            @Parameter(description = "ID del pedido", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.obtenerPorId(id)));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar el estado de un pedido")
    public ResponseEntity<ApiResponse<Void>> actualizarEstado(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long id,
            @RequestParam("estado") com.sistemagas.pedidos.enums.EstadoPedido estado) {
        pedidoService.actualizarEstado(id, estado);
        return ResponseEntity.ok(ApiResponse.ok(null, "Estado actualizado"));
    }

    @GetMapping("/uuid/{uuidOffline}")
    @Operation(summary = "Obtener un pedido por su UUID offline")
    public ResponseEntity<ApiResponse<PedidoResponse>> obtenerPorUuidOffline(
            @PathVariable @NotBlank(message = "uuidOffline no puede estar vacio") String uuidOffline) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.obtenerPorUuidOffline(uuidOffline)));
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir foto de fachada como evidencia visual",
            description = "Recibe un archivo de imagen (jpg/png/webp, max 10MB) y lo sube al bucket de Supabase Storage. "
                    + "Devuelve la URL publica resultante y la persiste en el pedido.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Foto subida y asociada al pedido",
                    content = @Content(schema = @Schema(implementation = PedidoFotoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Archivo invalido, vacio o tipo no permitido (solo image/jpeg, image/png, image/webp)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "Archivo excede el tamano maximo permitido (default 10MB)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "Error al comunicarse con Supabase Storage")
    })
    public ResponseEntity<ApiResponse<PedidoFotoResponse>> subirFoto(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long id,
            @Parameter(description = "Archivo de imagen (jpg/png/webp). En el form-data el campo debe llamarse 'archivo'.",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestParam("archivo") MultipartFile archivo,
            @Parameter(description = "Descripcion opcional de la evidencia (texto libre, no se persiste en Storage)")
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        PedidoFotoResponse response = pedidoService.subirFoto(id, archivo, descripcion);
        return ResponseEntity.ok(ApiResponse.ok(response, "Foto de evidencia subida correctamente"));
    }
}