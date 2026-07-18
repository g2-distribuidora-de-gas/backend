package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.AjusteInventarioRequest;
import com.sistemagas.pedidos.dto.request.DevolucionRequest;
import com.sistemagas.pedidos.dto.request.ReparacionFinRequest;
import com.sistemagas.pedidos.dto.request.ReparacionInicioRequest;
import com.sistemagas.pedidos.dto.request.RoturaRequest;
import com.sistemagas.pedidos.dto.request.VentaStockRequest;
import com.sistemagas.pedidos.dto.response.MovimientoResponse;
import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.enums.TipoMovimiento;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.MovimientoMapper;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.DepositoRepository;
import com.sistemagas.pedidos.repository.EstadoGarrafaRepository;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.service.InventarioService;
import com.sistemagas.pedidos.service.MovimientoStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final DepositoRepository depositoRepository;
    private final TipoGarrafaStockRepository tipoGarrafaRepository;
    private final EstadoGarrafaRepository estadoGarrafaRepository;
    private final PedidoRepository pedidoRepository;
    private final MovimientoStockService movimientoStockService;
    private final MovimientoMapper movimientoMapper;

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public List<MovimientoResponse> registrarVenta(VentaStockRequest req, Usuario usuario) {
        Deposito camion = obtenerDeposito(req.getCamionId());
        if (!TipoDeposito.CAMION.equals(camion.getTipo())) {
            throw new BusinessException("Solo se pueden registrar ventas desde un camión a través de este endpoint");
        }
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estadoLlena = obtenerEstado("LLENA");
        EstadoGarrafa estadoVacia = obtenerEstado("VACIA");
        Pedido pedido = req.getPedidoId() != null ? pedidoRepository.findById(req.getPedidoId()).orElse(null) : null;

        List<MovimientoResponse> respuestas = new ArrayList<>();

        // 1. Decrementar LLENAS
        if (req.getCantidadEntregadas() > 0) {
            movimientoStockService.decrementarStock(camion, tipo, estadoLlena, req.getCantidadEntregadas());
            MovimientoGarrafa movSalida = MovimientoGarrafa.builder()
                    .tipoMovimiento(TipoMovimiento.VENTA)
                    .depositoOrigen(camion)
                    .depositoDestino(null) // Sale del sistema hacia el cliente
                    .tipoGarrafa(tipo)
                    .estadoOrigen(estadoLlena)
                    .estadoDestino(estadoLlena)
                    .cantidad(req.getCantidadEntregadas())
                    .pedido(pedido)
                    .usuario(usuario)
                    .observaciones(req.getObservaciones())
                    .build();
            respuestas.add(movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(movSalida)));
        }

        // 2. Incrementar VACIAS
        if (req.getCantidadRecibidas() > 0) {
            movimientoStockService.incrementarStock(camion, tipo, estadoVacia, req.getCantidadRecibidas());
            MovimientoGarrafa movEntrada = MovimientoGarrafa.builder()
                    .tipoMovimiento(TipoMovimiento.DEVOLUCION) // Entra del cliente al sistema
                    .depositoOrigen(null)
                    .depositoDestino(camion)
                    .tipoGarrafa(tipo)
                    .estadoOrigen(estadoVacia)
                    .estadoDestino(estadoVacia)
                    .cantidad(req.getCantidadRecibidas())
                    .pedido(pedido)
                    .usuario(usuario)
                    .observaciones(req.getObservaciones())
                    .build();
            respuestas.add(movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(movEntrada)));
        }

        return respuestas;
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public MovimientoResponse registrarDevolucion(DevolucionRequest req, Usuario usuario) {
        Deposito destino = obtenerDeposito(req.getDepositoDestinoId());
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estado = obtenerEstadoGarrafa(req.getEstadoGarrafaId());
        Pedido pedido = req.getPedidoId() != null ? pedidoRepository.findById(req.getPedidoId()).orElse(null) : null;

        movimientoStockService.incrementarStock(destino, tipo, estado, req.getCantidad());

        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(TipoMovimiento.DEVOLUCION)
                .depositoOrigen(null)
                .depositoDestino(destino)
                .tipoGarrafa(tipo)
                .estadoOrigen(estado)
                .estadoDestino(estado)
                .cantidad(req.getCantidad())
                .pedido(pedido)
                .usuario(usuario)
                .observaciones(req.getObservaciones())
                .build();
        return movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov));
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public MovimientoResponse registrarRotura(RoturaRequest req, Usuario usuario) {
        Deposito deposito = obtenerDeposito(req.getDepositoId());
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estadoOriginal = obtenerEstadoGarrafa(req.getEstadoGarrafaId());
        EstadoGarrafa estadoRoto = obtenerEstado("FUERA_SERVICIO"); // O podría ser REPARACION según el caso

        // Si ya estaba rota, no hacemos nada especial, solo un registro (aunque es raro)
        if (!estadoOriginal.getCodigo().equals(estadoRoto.getCodigo())) {
            movimientoStockService.decrementarStock(deposito, tipo, estadoOriginal, req.getCantidad());
            movimientoStockService.incrementarStock(deposito, tipo, estadoRoto, req.getCantidad());
        }

        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(TipoMovimiento.ROTURA)
                .depositoOrigen(deposito)
                .depositoDestino(deposito)
                .tipoGarrafa(tipo)
                .estadoOrigen(estadoOriginal)
                .estadoDestino(estadoRoto)
                .cantidad(req.getCantidad())
                .usuario(usuario)
                .observaciones(req.getObservaciones())
                .build();
        return movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov));
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public MovimientoResponse iniciarReparacion(ReparacionInicioRequest req, Usuario usuario) {
        Deposito origen = obtenerDeposito(req.getDepositoOrigenId());
        Deposito taller = obtenerDeposito(req.getTallerDestinoId());
        if (!TipoDeposito.TALLER.equals(taller.getTipo())) {
            throw new BusinessException("El destino debe ser un TALLER");
        }
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estadoOrigen = obtenerEstadoGarrafa(req.getEstadoOrigenId());
        EstadoGarrafa estadoReparacion = obtenerEstado("REPARACION");

        movimientoStockService.decrementarStock(origen, tipo, estadoOrigen, req.getCantidad());
        movimientoStockService.incrementarStock(taller, tipo, estadoReparacion, req.getCantidad());

        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(TipoMovimiento.REPARACION_INICIO)
                .depositoOrigen(origen)
                .depositoDestino(taller)
                .tipoGarrafa(tipo)
                .estadoOrigen(estadoOrigen)
                .estadoDestino(estadoReparacion)
                .cantidad(req.getCantidad())
                .usuario(usuario)
                .observaciones(req.getObservaciones())
                .build();
        return movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov));
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public MovimientoResponse finalizarReparacion(ReparacionFinRequest req, Usuario usuario) {
        Deposito taller = obtenerDeposito(req.getTallerId());
        if (!TipoDeposito.TALLER.equals(taller.getTipo())) {
            throw new BusinessException("El origen debe ser un TALLER");
        }
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estadoReparacion = obtenerEstado("REPARACION");
        EstadoGarrafa estadoFinal = obtenerEstadoGarrafa(req.getEstadoFinalId());

        if (!estadoFinal.getCodigo().equals("LLENA") && !estadoFinal.getCodigo().equals("FUERA_SERVICIO") && !estadoFinal.getCodigo().equals("VACIA")) {
            throw new BusinessException("El estado final de una reparación debe ser LLENA, VACIA o FUERA_SERVICIO");
        }

        movimientoStockService.decrementarStock(taller, tipo, estadoReparacion, req.getCantidad());
        movimientoStockService.incrementarStock(taller, tipo, estadoFinal, req.getCantidad());

        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(TipoMovimiento.REPARACION_FIN)
                .depositoOrigen(taller)
                .depositoDestino(taller) // Se queda en el taller en el nuevo estado, luego se transfiere si es necesario
                .tipoGarrafa(tipo)
                .estadoOrigen(estadoReparacion)
                .estadoDestino(estadoFinal)
                .cantidad(req.getCantidad())
                .usuario(usuario)
                .observaciones(req.getObservaciones())
                .build();
        return movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov));
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public MovimientoResponse ajustarInventario(AjusteInventarioRequest req, Usuario usuario) {
        Deposito deposito = obtenerDeposito(req.getDepositoId());
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estado = obtenerEstadoGarrafa(req.getEstadoGarrafaId());

        if (AjusteInventarioRequest.TipoAjuste.ENTRADA.equals(req.getTipoAjuste())) {
            movimientoStockService.incrementarStock(deposito, tipo, estado, req.getCantidad());
        } else {
            movimientoStockService.decrementarStock(deposito, tipo, estado, req.getCantidad());
        }

        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(AjusteInventarioRequest.TipoAjuste.ENTRADA.equals(req.getTipoAjuste()) ? TipoMovimiento.AJUSTE_ENTRADA : TipoMovimiento.AJUSTE_SALIDA)
                .depositoOrigen(AjusteInventarioRequest.TipoAjuste.SALIDA.equals(req.getTipoAjuste()) ? deposito : null)
                .depositoDestino(AjusteInventarioRequest.TipoAjuste.ENTRADA.equals(req.getTipoAjuste()) ? deposito : null)
                .tipoGarrafa(tipo)
                .estadoOrigen(estado)
                .estadoDestino(estado)
                .cantidad(req.getCantidad())
                .usuario(usuario)
                .observaciones("Ajuste de inventario: " + req.getObservaciones())
                .build();
        return movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov));
    }

    // ----------------------------------------------------------------
    private Deposito obtenerDeposito(Long id) {
        return depositoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito no encontrado: id=" + id));
    }

    private TipoGarrafaStock obtenerTipoGarrafa(Long id) {
        return tipoGarrafaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de garrafa no encontrado: id=" + id));
    }

    private EstadoGarrafa obtenerEstadoGarrafa(Long id) {
        return estadoGarrafaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de garrafa no encontrado: id=" + id));
    }

    private EstadoGarrafa obtenerEstado(String codigo) {
        return estadoGarrafaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new BusinessException("Estado '" + codigo + "' no configurado en el sistema"));
    }
}
