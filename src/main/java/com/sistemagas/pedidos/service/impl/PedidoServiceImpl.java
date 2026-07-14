package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoDetalleResponse;
import com.sistemagas.pedidos.dto.response.PedidoFotoResponse;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapper;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.service.PedidoService;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import com.sistemagas.pedidos.util.Constantes;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final GarrafaRepositoryPort garrafaRepositoryPort;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoMapper pedidoMapper;
    private final PedidoDetalleMapper pedidoDetalleMapper;
    private final GarrafaStockHelper garrafaStockHelper;
    private final SupabaseStorageService supabaseStorageService;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request, String emailAutenticado) {
        if (request.getUuidOffline() != null
                && pedidoRepository.existsByUuidOffline(request.getUuidOffline())) {
            throw new BusinessException(Constantes.MSG_PEDIDO_DUPLICADO);
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        Usuario creador = null;
        if (request.getCreadorId() != null) {
            creador = usuarioRepository.findById(request.getCreadorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario creador no encontrado"));
        } else if (emailAutenticado != null) {
            creador = usuarioRepository.findByEmail(emailAutenticado)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado en base de datos"));
        }

        Pedido pedido = pedidoMapper.toEntity(request);
        pedido.setCliente(cliente);
        pedido.setCreador(creador);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        Map<Long, GarrafaModel> garrafas = garrafaStockHelper.cargarYValidar(request.getDetalles());

        for (PedidoDetalleRequest det : request.getDetalles()) {
            GarrafaModel garrafa = garrafas.get(det.getGarrafaId());
            if (garrafa == null) {
                throw new ResourceNotFoundException(Constantes.MSG_GARRAFA_NO_ENCONTRADA + ": id=" + det.getGarrafaId());
            }
            BigDecimal precioUnitario = garrafa.getPrecio();

            PedidoDetalle detalle = PedidoDetalle.builder()
                    .garrafaId(garrafa.getId())
                    .cantidad(det.getCantidad())
                    .precioUnitario(precioUnitario)
                    .build();

            pedido.agregarDetalle(detalle);

            garrafaStockHelper.validarYDescontar(garrafa, det.getCantidad());
            garrafaRepositoryPort.save(garrafa);
        }

        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido creado: id={}, uuidOffline={}, detalles={}",
                guardado.getId(), guardado.getUuidOffline(), guardado.getDetalles().size());

        return buildResponse(guardado, cliente, garrafas);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public PedidoResponse obtenerPorUuidOffline(String uuidOffline) {
        Pedido pedido = pedidoRepository.findByUuidOffline(uuidOffline)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));
        return buildResponseFor(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public PedidoResponse obtenerPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));
        return buildResponseFor(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))

    public List<PedidoResponse> listarTodos(Instant minUpdatedAt, Integer limit, EstadoPedido estado) {
        int pageLimit = (limit != null && limit > 0) ? limit : 100;
        Pageable pageable = PageRequest.of(0, pageLimit, Sort.by("updatedAt").ascending().and(Sort.by("id").ascending()));
        
        List<Pedido> pedidos;
        if (estado != null) {
            pedidos = (minUpdatedAt != null)
                    ? pedidoRepository.findByEstadoAndUpdatedAtGreaterThan(estado, minUpdatedAt, pageable)
                    : pedidoRepository.findByEstado(estado, pageable);
        } else {
            pedidos = (minUpdatedAt != null)
                    ? pedidoRepository.findByUpdatedAtGreaterThan(minUpdatedAt, pageable)
                    : pedidoRepository.findAllBy(pageable);
        }

        return pedidos.stream()
                .map(this::buildResponseFor)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))

    public List<PedidoResponse> listarPorCreador(Long creadorId, Instant minUpdatedAt, Integer limit, EstadoPedido estado) {
        int pageLimit = (limit != null && limit > 0) ? limit : 100;
        Pageable pageable = PageRequest.of(0, pageLimit, Sort.by("updatedAt").ascending().and(Sort.by("id").ascending()));
        
        List<Pedido> pedidos;
        if (estado != null) {
            pedidos = (minUpdatedAt != null)
                    ? pedidoRepository.findByCreadorIdAndEstadoAndUpdatedAtGreaterThan(creadorId, estado, minUpdatedAt, pageable)
                    : pedidoRepository.findByCreadorIdAndEstado(creadorId, estado, pageable);
        } else {
            pedidos = (minUpdatedAt != null)
                    ? pedidoRepository.findByCreadorIdAndUpdatedAtGreaterThan(creadorId, minUpdatedAt, pageable)
                    : pedidoRepository.findByCreadorId(creadorId, pageable);
        }

        return pedidos.stream()
                .map(this::buildResponseFor)
                .toList();
    }

    @Override
    @Transactional
    public void actualizarEstado(Long id, EstadoPedido estado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));
        pedido.setEstado(estado);
        pedidoRepository.save(pedido);
    }

    @Override
    @Deprecated
    public PedidoFotoResponse subirFoto(Long id, MultipartFile archivo, String descripcion) {
        log.warn("DEPRECATED: POST /api/pedidos/{{}}/foto debe migrarse a POST /api/clientes/{{clienteId}}/foto. "
                + "Esta llamada se mantiene por compatibilidad de la app mobile hasta que actualice.", id);

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));

        String objectPath = supabaseStorageService.subir(id, archivo, descripcion);

        try {
            return transactionTemplate.execute(status ->
                    buildLegacyFotoResponse(pedido, objectPath, archivo));
        } catch (RuntimeException ex) {
            log.error("Fallo generando signed URL para foto legacy del pedido id={}. Compensando: eliminando {}",
                    id, objectPath, ex);
            supabaseStorageService.eliminar(objectPath);
            throw ex;
        }
    }

    private PedidoFotoResponse buildLegacyFotoResponse(Pedido pedido, String objectPath, MultipartFile archivo) {
        String signedUrl = supabaseStorageService.getSignedUrl(objectPath);

        log.info("Foto de evidencia legacy servida (no persistida en DB) para pedido id={}, objectPath={}",
                pedido.getId(), objectPath);

        return PedidoFotoResponse.builder()
                .pedidoId(pedido.getId())
                .urlFotoEvidencia(signedUrl)
                .nombreArchivo(archivo.getOriginalFilename())
                .contentType(archivo.getContentType())
                .tamanioBytes(archivo.getSize())
                .build();
    }

    private PedidoResponse buildResponseFor(Pedido pedido) {
        Cliente cliente = pedido.getCliente();

        Map<Long, GarrafaModel> garrafas = new HashMap<>();
        for (PedidoDetalle d : pedido.getDetalles()) {
            garrafaRepositoryPort.findById(d.getGarrafaId()).ifPresent(g -> garrafas.put(g.getId(), g));
        }

        return buildResponse(pedido, cliente, garrafas);
    }

    private PedidoResponse buildResponse(Pedido pedido, Cliente cliente, Map<Long, GarrafaModel> garrafas) {
        PedidoResponse response = pedidoMapper.toResponse(pedido);

        if (cliente != null && cliente.getFotoEvidenciaPath() != null) {
            response.setUrlFotoEvidencia(
                    supabaseStorageService.getSignedUrl(cliente.getFotoEvidenciaPath()));
        }

        if (pedido.getCreador() != null) {
            response.setCreadorId(pedido.getCreador().getId());
            response.setCreadorNombre(pedido.getCreador().getNombre() + " " + pedido.getCreador().getApellido());
        }

        List<PedidoDetalleResponse> detalles = new ArrayList<>();
        for (PedidoDetalle d : pedido.getDetalles()) {
            PedidoDetalleResponse detResp = pedidoDetalleMapper.toResponse(d);
            pedidoDetalleMapper.applyGarrafaTipo(detResp, garrafas.get(d.getGarrafaId()));
            detalles.add(detResp);
        }
        response.setDetalles(detalles);
        return response;
    }
}