package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.GarrafaRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.GarrafaResponse;
import com.sistemagas.pedidos.service.GarrafaService;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.Instant;

@RestController
@RequestMapping(Constantes.API_GARRAFAS)
@RequiredArgsConstructor
@Tag(name = "Garrafas", description = "API para la gestión del catálogo de garrafas")
public class GarrafaController {

    private final GarrafaService garrafaService;

    @GetMapping
    @Operation(summary = "Listar todas las garrafas", description = "Retorna el catálogo completo de garrafas disponibles")
    public ResponseEntity<ApiResponse<List<GarrafaResponse>>> listarTodas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant minUpdatedAt,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        List<GarrafaResponse> garrafas = garrafaService.listarTodas(minUpdatedAt, limit);
        return ResponseEntity.ok(ApiResponse.ok(garrafas));
    }

    @PostMapping
    @Operation(summary = "Crear nueva garrafa", description = "Crea una nueva garrafa en el catálogo")
    public ResponseEntity<ApiResponse<GarrafaResponse>> crear(@Valid @RequestBody GarrafaRequest request) {
        GarrafaResponse response = garrafaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar garrafa", description = "Actualiza los datos de una garrafa existente")
    public ResponseEntity<ApiResponse<GarrafaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GarrafaRequest request) {
        GarrafaResponse response = garrafaService.actualizar(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
