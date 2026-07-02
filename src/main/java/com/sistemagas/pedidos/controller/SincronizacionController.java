package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionEstadoResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.service.SincronizacionService;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constantes.API_SINCRONIZAR)
@RequiredArgsConstructor
@Tag(name = "Sincronizacion", description = "Sincronizacion de pedidos creados offline")
public class SincronizacionController {

    private final SincronizacionService sincronizacionService;

    @PostMapping
    @Operation(summary = "Sincronizar lote de pedidos creados offline",
            description = "Recibe pedidos guardados en IndexedDB del cliente y los procesa. " +
                    "Devuelve 3 listas: procesados, duplicados y errores.")
    public ResponseEntity<ApiResponse<SincronizacionResponse>> sincronizar(
            @Valid @RequestBody SincronizacionRequest request) {
        SincronizacionResponse response = sincronizacionService.procesarPedidosOffline(request);
        return ResponseEntity.ok(ApiResponse.ok(response, Constantes.MSG_SINCRONIZACION_OK));
    }

    @GetMapping("/estado")
    @Operation(summary = "Consultar el estado de sincronizacion de una lista de UUIDs",
            description = "Recibe una lista de UUIDs offline y devuelve cuales ya fueron procesados " +
                    "en el servidor y cuales aun no. Util para que el cliente consulte antes de reenviar.")
    public ResponseEntity<ApiResponse<SincronizacionEstadoResponse>> consultarEstado(
            @Parameter(description = "Lista de UUIDs offline a consultar (separados por coma)",
                    example = "uuid-1,uuid-2,uuid-3")
            @RequestParam("uuids") List<String> uuids) {
        return ResponseEntity.ok(ApiResponse.ok(sincronizacionService.consultarEstado(uuids)));
    }
}