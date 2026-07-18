package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.response.StockDepositoResponse;
import com.sistemagas.pedidos.dto.response.StockTotalResponse;
import com.sistemagas.pedidos.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Consultas de stock (depósitos, camiones y total general)")
public class StockController {

    private final StockService stockService;

    @GetMapping("/depositos/{depositoId}")
    @Operation(summary = "Obtener stock de un depósito específico")
    public ResponseEntity<StockDepositoResponse> getStockByDeposito(
            @Parameter(description = "ID del depósito") @PathVariable Long depositoId) {
        return ResponseEntity.ok(stockService.getStockByDeposito(depositoId));
    }

    @GetMapping("/camiones/{camionId}")
    @Operation(summary = "Obtener stock de un camión específico")
    public ResponseEntity<StockDepositoResponse> getStockCamion(
            @Parameter(description = "ID del camión") @PathVariable Long camionId) {
        return ResponseEntity.ok(stockService.getStockCamion(camionId));
    }

    @GetMapping("/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Obtener stock total del sistema",
            description = "Calcula en tiempo real la suma del stock de todos los depósitos activos.")
    public ResponseEntity<StockTotalResponse> getStockTotal() {
        return ResponseEntity.ok(stockService.getStockTotal());
    }
}
