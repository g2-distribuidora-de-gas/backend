package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionEstadoResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.service.SincronizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionServiceImpl implements SincronizacionService {

    private final PedidoRepository pedidoRepository;
    private final SincronizacionPedidoProcessor pedidoProcessor;

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public SincronizacionResponse procesarPedidosOffline(SincronizacionRequest request, String emailAutenticado) {
        log.info("Iniciando sincronizacion offline. Pedidos recibidos: {}", request.getPedidos().size());

        SincronizacionResponse response = SincronizacionResponse.builder()
                .servidorFecha(Instant.now())
                .total(request.getPedidos().size())
                .procesados(new ArrayList<>())
                .duplicados(new ArrayList<>())
                .errores(new ArrayList<>())
                .build();

        List<String> uuids = request.getPedidos().stream()
                .map(PedidoRequest::getUuidOffline)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .toList();

        Map<String, Pedido> existentes = uuids.isEmpty()
                ? Map.of()
                : pedidoRepository.findByUuidOfflineIn(uuids).stream()
                        .collect(Collectors.toMap(
                                Pedido::getUuidOffline,
                                Function.identity(),
                                (existing, duplicate) -> existing));

        for (PedidoRequest pedidoReq : request.getPedidos()) {
            try {
                procesarPedidoIndividual(pedidoReq, existentes, response, emailAutenticado);
            } catch (Exception ex) {
                log.error("Error procesando pedido uuidOffline={}: {}",
                        pedidoReq.getUuidOffline(), ex.getMessage(), ex);
                response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                        .uuidOffline(pedidoReq.getUuidOffline())
                        .motivo("Error inesperado al procesar el pedido")
                        .build());
            }
        }

        log.info("Sincronizacion finalizada. Procesados: {}, Duplicados: {}, Errores: {}",
                response.getProcesados().size(),
                response.getDuplicados().size(),
                response.getErrores().size());

        return response;
    }

    private void procesarPedidoIndividual(PedidoRequest request,
                                           Map<String, Pedido> existentes,
                                           SincronizacionResponse response,
                                           String emailAutenticado) {

        if (request.getUuidOffline() == null || request.getUuidOffline().isBlank()) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .motivo("uuidOffline es obligatorio para sincronizacion offline")
                    .build());
            return;
        }

        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo("El pedido debe tener al menos un detalle")
                    .build());
            return;
        }

        if (existentes.containsKey(request.getUuidOffline())) {
            response.getDuplicados().add(request.getUuidOffline());
            return;
        }

        try {
            SincronizacionResponse.Procesado procesado = pedidoProcessor.procesar(request, emailAutenticado);
            response.getProcesados().add(procesado);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Conflicto de integridad al procesar pedido uuidOffline={}: {}",
                    request.getUuidOffline(), ex.getMostSpecificCause().getMessage());
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo("Pedido duplicado (uuidOffline ya existe)")
                    .build());
        } catch (IllegalStateException ex) {
            response.getErrores().add(SincronizacionResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo(ex.getMessage())
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public SincronizacionEstadoResponse consultarEstado(List<String> uuids) {
        List<String> uuidsLimpios = uuids == null
                ? List.of()
                : uuids.stream()
                        .filter(uuid -> uuid != null && !uuid.isBlank())
                        .distinct()
                        .toList();

        if (uuidsLimpios.isEmpty()) {
            return SincronizacionEstadoResponse.builder()
                    .totalConsultados(0)
                    .encontrados(0)
                    .procesados(new ArrayList<>())
                    .noEncontrados(new ArrayList<>())
                    .build();
        }

        Set<String> existentes = pedidoRepository.findByUuidOfflineIn(uuidsLimpios).stream()
                .map(Pedido::getUuidOffline)
                .collect(Collectors.toSet());

        List<String> procesados = new ArrayList<>();
        List<String> noEncontrados = new ArrayList<>();
        for (String uuid : uuidsLimpios) {
            if (existentes.contains(uuid)) {
                procesados.add(uuid);
            } else {
                noEncontrados.add(uuid);
            }
        }

        return SincronizacionEstadoResponse.builder()
                .totalConsultados(uuidsLimpios.size())
                .encontrados(procesados.size())
                .procesados(procesados)
                .noEncontrados(noEncontrados)
                .build();
    }
}