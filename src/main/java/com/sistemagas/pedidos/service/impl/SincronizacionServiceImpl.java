package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionEstadoResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.service.SincronizacionService;
import com.sistemagas.pedidos.util.Constantes;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sistemagas.pedidos.dto.request.ClienteRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionClienteRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionClienteResponse;
import com.sistemagas.pedidos.dto.request.SincronizacionParadasRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionParadaItemRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionParadasResponse;
import com.sistemagas.pedidos.dto.request.SincronizacionRutaItemRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRutasRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionRutasResponse;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.model.Cliente;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionServiceImpl implements SincronizacionService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final SincronizacionPedidoProcessor pedidoProcessor;
    private final SincronizacionClienteProcessor clienteProcessor;
    private final SincronizacionParadaProcessor paradaProcessor;
    private final SincronizacionRutaProcessor rutaProcessor;

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

        // Detecta duplicados intra-batch ANTES de delegar al processor.
        // Si el mismo uuidOffline aparece 2+ veces en el mismo request, solo se
        // procesa la primera ocurrencia; las siguientes van directo a "duplicados"
        // y no generan DataIntegrityViolationException.
        java.util.Set<String> uuidsVistosEnBatch = new java.util.HashSet<>();

        for (PedidoRequest pedidoReq : request.getPedidos()) {
            String uuidBatch = pedidoReq.getUuidOffline();
            try {
                if (uuidBatch != null && !uuidBatch.isBlank()
                        && !uuidsVistosEnBatch.add(uuidBatch)) {
                    response.getDuplicados().add(uuidBatch);
                    continue;
                }
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

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public SincronizacionClienteResponse procesarClientesOffline(SincronizacionClienteRequest request) {
        log.info("Iniciando sincronizacion offline de clientes. Recibidos: {}", request.getClientes().size());

        SincronizacionClienteResponse response = SincronizacionClienteResponse.builder()
                .servidorFecha(Instant.now())
                .total(request.getClientes().size())
                .procesados(new ArrayList<>())
                .duplicados(new ArrayList<>())
                .errores(new ArrayList<>())
                .build();

        List<String> uuids = request.getClientes().stream()
                .map(ClienteRequest::getUuidOffline)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .toList();

        Map<String, Cliente> existentes = uuids.isEmpty()
                ? Map.of()
                : clienteRepository.findByUuidOfflineIn(uuids).stream()
                        .collect(Collectors.toMap(
                                Cliente::getUuidOffline,
                                Function.identity(),
                                (existing, duplicate) -> existing));

        // Detecta duplicados intra-batch ANTES de delegar al processor.
        // Mismo patron que en pedidos: solo se procesa la primera ocurrencia.
        java.util.Set<String> uuidsVistosEnBatch = new java.util.HashSet<>();

        for (ClienteRequest clienteReq : request.getClientes()) {
            String uuidBatch = clienteReq.getUuidOffline();
            try {
                if (uuidBatch != null && !uuidBatch.isBlank()
                        && !uuidsVistosEnBatch.add(uuidBatch)) {
                    response.getDuplicados().add(uuidBatch);
                    continue;
                }
                procesarClienteIndividual(clienteReq, existentes, response);
            } catch (Exception ex) {
                log.error("Error procesando cliente uuidOffline={}: {}",
                        clienteReq.getUuidOffline(), ex.getMessage(), ex);
                response.getErrores().add(SincronizacionClienteResponse.ErrorItem.builder()
                        .uuidOffline(clienteReq.getUuidOffline())
                        .motivo("Error inesperado al procesar el cliente")
                        .build());
            }
        }

        log.info("Sincronizacion de clientes finalizada. Procesados: {}, Duplicados: {}, Errores: {}",
                response.getProcesados().size(),
                response.getDuplicados().size(),
                response.getErrores().size());

        return response;
    }

    private void procesarClienteIndividual(ClienteRequest request,
                                           Map<String, Cliente> existentes,
                                           SincronizacionClienteResponse response) {

        if (request.getUuidOffline() == null || request.getUuidOffline().isBlank()) {
            response.getErrores().add(SincronizacionClienteResponse.ErrorItem.builder()
                    .motivo("uuidOffline es obligatorio para sincronizacion offline")
                    .build());
            return;
        }

        if (existentes.containsKey(request.getUuidOffline())) {
            response.getDuplicados().add(request.getUuidOffline());
            return;
        }

        try {
            SincronizacionClienteResponse.Procesado procesado = clienteProcessor.procesar(request);
            response.getProcesados().add(procesado);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Conflicto de integridad al procesar cliente uuidOffline={}: {}",
                    request.getUuidOffline(), ex.getMostSpecificCause().getMessage());
            response.getErrores().add(SincronizacionClienteResponse.ErrorItem.builder()
                    .uuidOffline(request.getUuidOffline())
                    .motivo("Cliente duplicado (uuidOffline ya existe)")
                    .build());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            response.getErrores().add(SincronizacionClienteResponse.ErrorItem.builder()
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
    public SincronizacionEstadoResponse consultarEstadoClientes(List<String> uuids) {
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

        Set<String> existentes = clienteRepository.findByUuidOfflineIn(uuidsLimpios).stream()
                .map(Cliente::getUuidOffline)
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

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public SincronizacionParadasResponse procesarParadasOffline(SincronizacionParadasRequest request, String emailAutenticado) {
        log.info("Iniciando sincronizacion offline de paradas. Recibidas: {}", request.getParadas().size());

        List<SincronizacionParadasResponse.ParadaProcesada> procesados = new ArrayList<>();
        List<SincronizacionParadasResponse.ParadaError> errores = new ArrayList<>();

        Set<String> uuidsVistosEnBatch = new HashSet<>();
        Set<Long> paradasVistasEnBatch = new HashSet<>();

        for (SincronizacionParadaItemRequest item : request.getParadas()) {
            try {
                if (item.getUuidOffline() == null || item.getUuidOffline().isBlank()) {
                    errores.add(SincronizacionParadasResponse.ParadaError.builder()
                            .rutaPedidoId(item.getRutaPedidoId())
                            .error("uuidOffline es obligatorio")
                            .build());
                    continue;
                }
                if (item.getRutaPedidoId() == null) {
                    errores.add(SincronizacionParadasResponse.ParadaError.builder()
                            .uuidOffline(item.getUuidOffline())
                            .error("rutaPedidoId es obligatorio")
                            .build());
                    continue;
                }
                if (!uuidsVistosEnBatch.add(item.getUuidOffline())) {
                    log.debug("Parada con uuidOffline={} duplicada en el batch, contando como procesado",
                            item.getUuidOffline());
                    procesados.add(procesada(item));
                    continue;
                }
                if (!paradasVistasEnBatch.add(item.getRutaPedidoId())) {
                    log.debug("Parada con rutaPedidoId={} duplicada en el batch (UUID distinto), contando como procesado",
                            item.getRutaPedidoId());
                    procesados.add(procesada(item));
                    continue;
                }
                procesarParadaIndividual(item, emailAutenticado, procesados, errores);
            } catch (Exception ex) {
                log.error("Error inesperado procesando parada uuidOffline={}",
                        item.getUuidOffline(), ex);
                errores.add(SincronizacionParadasResponse.ParadaError.builder()
                        .uuidOffline(item.getUuidOffline())
                        .rutaPedidoId(item.getRutaPedidoId())
                        .error("Error inesperado al procesar la parada")
                        .build());
            }
        }

        return SincronizacionParadasResponse.builder()
                .procesados(procesados)
                .errores(errores)
                .build();
    }

    private void procesarParadaIndividual(SincronizacionParadaItemRequest item,
                                          String emailAutenticado,
                                          List<SincronizacionParadasResponse.ParadaProcesada> procesados,
                                          List<SincronizacionParadasResponse.ParadaError> errores) {
        try {
            paradaProcessor.procesarParada(item, emailAutenticado);
            procesados.add(procesada(item));
        } catch (ResourceNotFoundException ex) {
            errores.add(SincronizacionParadasResponse.ParadaError.builder()
                    .uuidOffline(item.getUuidOffline())
                    .rutaPedidoId(item.getRutaPedidoId())
                    .error(ex.getMessage())
                    .build());
        } catch (BusinessException ex) {
            if (esEstadoNoCambiable(ex)) {
                log.debug("Parada {} ya estaba en estado distinto a PENDIENTE, contando como procesado",
                        item.getRutaPedidoId());
                procesados.add(procesada(item));
            } else {
                errores.add(SincronizacionParadasResponse.ParadaError.builder()
                        .uuidOffline(item.getUuidOffline())
                        .rutaPedidoId(item.getRutaPedidoId())
                        .error(ex.getMessage())
                        .build());
            }
        }
    }

    private boolean esEstadoNoCambiable(BusinessException ex) {
        if (ex.getMessage() == null) {
            return false;
        }
        String prefijo = Constantes.MSG_ESTADO_NO_CAMBIABLE.split("%s")[0];
        return ex.getMessage().startsWith(prefijo.trim());
    }

    private SincronizacionParadasResponse.ParadaProcesada procesada(SincronizacionParadaItemRequest item) {
        return SincronizacionParadasResponse.ParadaProcesada.builder()
                .uuidOffline(item.getUuidOffline())
                .rutaPedidoId(item.getRutaPedidoId())
                .build();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public SincronizacionRutasResponse procesarRutasOffline(SincronizacionRutasRequest request, String emailAutenticado) {
        log.info("Iniciando sincronizacion offline de rutas. Recibidos: {}", request.getCambios().size());

        List<SincronizacionRutasResponse.RutaProcesada> procesados = new ArrayList<>();
        List<SincronizacionRutasResponse.RutaError> errores = new ArrayList<>();

        Set<String> uuidsVistosEnBatch = new HashSet<>();
        Set<Long> rutasVistasEnBatch = new HashSet<>();

        for (SincronizacionRutaItemRequest item : request.getCambios()) {
            try {
                if (item.getUuidOffline() == null || item.getUuidOffline().isBlank()) {
                    errores.add(SincronizacionRutasResponse.RutaError.builder()
                            .rutaId(item.getRutaId())
                            .error("uuidOffline es obligatorio")
                            .build());
                    continue;
                }
                if (item.getRutaId() == null) {
                    errores.add(SincronizacionRutasResponse.RutaError.builder()
                            .uuidOffline(item.getUuidOffline())
                            .error("rutaId es obligatorio")
                            .build());
                    continue;
                }
                if (!uuidsVistosEnBatch.add(item.getUuidOffline())) {
                    log.debug("Cambio de ruta con uuidOffline={} duplicado en el batch, contando como procesado",
                            item.getUuidOffline());
                    procesados.add(procesadaRuta(item));
                    continue;
                }
                if (!rutasVistasEnBatch.add(item.getRutaId())) {
                    log.debug("Cambio de ruta con rutaId={} duplicado en el batch (UUID distinto), contando como procesado",
                            item.getRutaId());
                    procesados.add(procesadaRuta(item));
                    continue;
                }
                rutaProcessor.procesarRuta(item, emailAutenticado);
                procesados.add(procesadaRuta(item));
            } catch (ResourceNotFoundException ex) {
                errores.add(SincronizacionRutasResponse.RutaError.builder()
                        .uuidOffline(item.getUuidOffline())
                        .rutaId(item.getRutaId())
                        .error(ex.getMessage())
                        .build());
            } catch (BusinessException ex) {
                errores.add(SincronizacionRutasResponse.RutaError.builder()
                        .uuidOffline(item.getUuidOffline())
                        .rutaId(item.getRutaId())
                        .error(ex.getMessage())
                        .build());
            } catch (Exception ex) {
                log.error("Error inesperado procesando cambio de ruta uuidOffline={}",
                        item.getUuidOffline(), ex);
                errores.add(SincronizacionRutasResponse.RutaError.builder()
                        .uuidOffline(item.getUuidOffline())
                        .rutaId(item.getRutaId())
                        .error("Error inesperado al procesar el cambio de ruta")
                        .build());
            }
        }

        return SincronizacionRutasResponse.builder()
                .procesados(procesados)
                .errores(errores)
                .build();
    }

    private SincronizacionRutasResponse.RutaProcesada procesadaRuta(SincronizacionRutaItemRequest item) {
        return SincronizacionRutasResponse.RutaProcesada.builder()
                .uuidOffline(item.getUuidOffline())
                .rutaId(item.getRutaId())
                .build();
    }
}