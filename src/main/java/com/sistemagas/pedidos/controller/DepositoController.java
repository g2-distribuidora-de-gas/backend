package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.DepositoRequest;
import com.sistemagas.pedidos.dto.response.DepositoResponse;
import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.service.DepositoService;
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
@RequestMapping("/api/depositos")
@RequiredArgsConstructor
@Tag(name = "Depósitos", description = "Gestión de depósitos, sucursales, plantas y camiones")
public class DepositoController {

    private final DepositoService depositoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Crear depósito",
            description = "Registra una nueva ubicación de stock (depósito, camión, sucursal, planta o taller).")
    public ResponseEntity<DepositoResponse> crear(@Valid @RequestBody DepositoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depositoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Actualizar depósito",
            description = "Modifica los datos de un depósito existente.")
    public ResponseEntity<DepositoResponse> actualizar(
            @Parameter(description = "ID del depósito") @PathVariable Long id,
            @Valid @RequestBody DepositoRequest request) {
        return ResponseEntity.ok(depositoService.actualizar(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener depósito por ID")
    public ResponseEntity<DepositoResponse> obtener(
            @Parameter(description = "ID del depósito") @PathVariable Long id) {
        return ResponseEntity.ok(depositoService.obtenerPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar depósitos",
            description = "Retorna todos los depósitos. Filtrar por tipo con el parámetro ?tipo=CAMION.")
    public ResponseEntity<List<DepositoResponse>> listar(
            @Parameter(description = "Filtrar por tipo (opcional). Valores: DEPOSITO_CENTRAL, CAMION, SUCURSAL, PLANTA, TALLER")
            @RequestParam(required = false) TipoDeposito tipo,
            @Parameter(description = "Solo activos (default: true)")
            @RequestParam(defaultValue = "true") boolean soloActivos) {

        if (tipo != null) {
            return ResponseEntity.ok(depositoService.listarPorTipo(tipo));
        }
        List<DepositoResponse> resultado = soloActivos
                ? depositoService.listarActivos()
                : depositoService.listarTodos();
        return ResponseEntity.ok(resultado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Activar o desactivar depósito",
            description = "Cambia el estado activo/inactivo del depósito sin eliminarlo.")
    public ResponseEntity<DepositoResponse> cambiarEstado(
            @Parameter(description = "ID del depósito") @PathVariable Long id,
            @Parameter(description = "true para activar, false para desactivar")
            @RequestParam boolean activo) {
        return ResponseEntity.ok(depositoService.cambiarEstado(id, activo));
    }
}
