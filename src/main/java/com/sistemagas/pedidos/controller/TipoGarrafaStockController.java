package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.TipoGarrafaStockRequest;
import com.sistemagas.pedidos.dto.response.EstadoGarrafaResponse;
import com.sistemagas.pedidos.dto.response.TipoGarrafaStockResponse;
import com.sistemagas.pedidos.service.TipoGarrafaStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-garrafa-stock")
@RequiredArgsConstructor
@Tag(name = "Tipos y Estados de Garrafa (Stock)", description = "Catálogo de tipos y estados de garrafas para el módulo de stock")
public class TipoGarrafaStockController {

    private final TipoGarrafaStockService tipoGarrafaStockService;

    // ----------------------------------------------------------------
    // Tipos de garrafa
    // ----------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Crear tipo de garrafa",
            description = "Registra un nuevo tipo de garrafa en el catálogo de stock.")
    public ResponseEntity<TipoGarrafaStockResponse> crear(@Valid @RequestBody TipoGarrafaStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoGarrafaStockService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Actualizar tipo de garrafa")
    public ResponseEntity<TipoGarrafaStockResponse> actualizar(
            @Parameter(description = "ID del tipo de garrafa") @PathVariable Long id,
            @Valid @RequestBody TipoGarrafaStockRequest request) {
        return ResponseEntity.ok(tipoGarrafaStockService.actualizar(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tipo de garrafa por ID")
    public ResponseEntity<TipoGarrafaStockResponse> obtener(
            @Parameter(description = "ID del tipo de garrafa") @PathVariable Long id) {
        return ResponseEntity.ok(tipoGarrafaStockService.obtenerPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar tipos de garrafa",
            description = "Retorna todos los tipos de garrafa. Por defecto solo los activos.")
    public ResponseEntity<List<TipoGarrafaStockResponse>> listar(
            @Parameter(description = "Solo activos (default: true)")
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        List<TipoGarrafaStockResponse> resultado = soloActivos
                ? tipoGarrafaStockService.listarActivos()
                : tipoGarrafaStockService.listarTodos();
        return ResponseEntity.ok(resultado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Activar o desactivar tipo de garrafa")
    public ResponseEntity<TipoGarrafaStockResponse> cambiarEstado(
            @Parameter(description = "ID del tipo de garrafa") @PathVariable Long id,
            @RequestParam boolean activo) {
        return ResponseEntity.ok(tipoGarrafaStockService.cambiarEstado(id, activo));
    }

    // ----------------------------------------------------------------
    // Estados de garrafa (catálogo de solo lectura)
    // ----------------------------------------------------------------

    @GetMapping("/estados")
    @Operation(summary = "Listar estados de garrafa",
            description = "Retorna el catálogo de estados posibles: LLENA, VACIA, RESERVADA, REPARACION, FUERA_SERVICIO.")
    public ResponseEntity<List<EstadoGarrafaResponse>> listarEstados() {
        return ResponseEntity.ok(tipoGarrafaStockService.listarEstados());
    }
}
