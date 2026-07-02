package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.service.PedidoService;
import com.sistemagas.pedidos.util.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<List<PedidoResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.listarTodos()));
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

    @GetMapping("/uuid/{uuidOffline}")
    @Operation(summary = "Obtener un pedido por su UUID offline")
    public ResponseEntity<ApiResponse<PedidoResponse>> obtenerPorUuidOffline(
            @PathVariable @NotBlank(message = "uuidOffline no puede estar vacio") String uuidOffline) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.obtenerPorUuidOffline(uuidOffline)));
    }
}