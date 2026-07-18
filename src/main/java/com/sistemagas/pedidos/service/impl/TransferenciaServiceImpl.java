package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.CargaCamionRequest;
import com.sistemagas.pedidos.dto.request.DescargaCamionRequest;
import com.sistemagas.pedidos.dto.request.TransferenciaRequest;
import com.sistemagas.pedidos.dto.response.MovimientoResponse;
import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.enums.TipoMovimiento;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.MovimientoMapper;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.DepositoRepository;
import com.sistemagas.pedidos.repository.EstadoGarrafaRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.service.MovimientoStockService;
import com.sistemagas.pedidos.service.TransferenciaService;
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
public class TransferenciaServiceImpl implements TransferenciaService {

    private final DepositoRepository depositoRepository;
    private final TipoGarrafaStockRepository tipoGarrafaRepository;
    private final EstadoGarrafaRepository estadoGarrafaRepository;
    private final MovimientoStockService movimientoStockService;
    private final MovimientoMapper movimientoMapper;

    @Override
    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public MovimientoResponse transferir(TransferenciaRequest req, Usuario usuario) {
        if (req.getDepositoOrigenId().equals(req.getDepositoDestinoId())) {
            throw new BusinessException("El depósito origen y destino no pueden ser el mismo");
        }

        Deposito origen = obtenerDeposito(req.getDepositoOrigenId());
        Deposito destino = obtenerDeposito(req.getDepositoDestinoId());
        TipoGarrafaStock tipo = obtenerTipoGarrafa(req.getTipoGarrafaId());
        EstadoGarrafa estado = obtenerEstadoGarrafa(req.getEstadoGarrafaId());

        validarActivos(origen, destino, tipo, estado);

        // 1. Decrementar origen
        movimientoStockService.decrementarStock(origen, tipo, estado, req.getCantidad());
        // 2. Incrementar destino
        movimientoStockService.incrementarStock(destino, tipo, estado, req.getCantidad());

        // 3. Registrar movimiento
        MovimientoGarrafa mov = MovimientoGarrafa.builder()
                .tipoMovimiento(TipoMovimiento.TRANSFERENCIA)
                .depositoOrigen(origen)
                .depositoDestino(destino)
                .tipoGarrafa(tipo)
                .estadoOrigen(estado)
                .estadoDestino(estado) // No hay cambio de estado en transferencia simple
                .cantidad(req.getCantidad())
                .usuario(usuario)
                .observaciones(req.getObservaciones())
                .build();

        MovimientoGarrafa guardado = movimientoStockService.registrarMovimiento(mov);
        return movimientoMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public List<MovimientoResponse> cargarCamion(CargaCamionRequest req, Usuario usuario) {
        Deposito camion = obtenerDeposito(req.getCamionId());
        Deposito central = obtenerDeposito(req.getDepositoCentralId());

        if (!TipoDeposito.CAMION.equals(camion.getTipo())) {
            throw new BusinessException("El depósito destino debe ser un CAMION");
        }
        if (!TipoDeposito.DEPOSITO_CENTRAL.equals(central.getTipo()) && !TipoDeposito.PLANTA.equals(central.getTipo())) {
            throw new BusinessException("El origen de la carga debe ser un DEPOSITO_CENTRAL o PLANTA");
        }

        // La carga de camión siempre asume garrafas en estado LLENA
        EstadoGarrafa estadoLlena = estadoGarrafaRepository.findByCodigo("LLENA")
                .orElseThrow(() -> new BusinessException("Estado LLENA no configurado en el sistema"));

        List<MovimientoResponse> respuestas = new ArrayList<>();

        for (CargaCamionRequest.ItemCarga item : req.getItems()) {
            TipoGarrafaStock tipo = obtenerTipoGarrafa(item.getTipoGarrafaId());
            validarActivos(central, camion, tipo, estadoLlena);

            movimientoStockService.decrementarStock(central, tipo, estadoLlena, item.getCantidad());
            movimientoStockService.incrementarStock(camion, tipo, estadoLlena, item.getCantidad());

            MovimientoGarrafa mov = MovimientoGarrafa.builder()
                    .tipoMovimiento(TipoMovimiento.CARGA_CAMION)
                    .depositoOrigen(central)
                    .depositoDestino(camion)
                    .tipoGarrafa(tipo)
                    .estadoOrigen(estadoLlena)
                    .estadoDestino(estadoLlena)
                    .cantidad(item.getCantidad())
                    .usuario(usuario)
                    .observaciones(req.getObservaciones())
                    .build();

            respuestas.add(movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov)));
        }
        return respuestas;
    }

    @Override
    @Transactional
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public List<MovimientoResponse> descargarCamion(DescargaCamionRequest req, Usuario usuario) {
        Deposito camion = obtenerDeposito(req.getCamionId());
        Deposito central = obtenerDeposito(req.getDepositoCentralId());

        if (!TipoDeposito.CAMION.equals(camion.getTipo())) {
            throw new BusinessException("El origen de la descarga debe ser un CAMION");
        }
        if (!TipoDeposito.DEPOSITO_CENTRAL.equals(central.getTipo()) && !TipoDeposito.PLANTA.equals(central.getTipo())) {
            throw new BusinessException("El destino de la descarga debe ser un DEPOSITO_CENTRAL o PLANTA");
        }

        List<MovimientoResponse> respuestas = new ArrayList<>();

        for (DescargaCamionRequest.ItemDescarga item : req.getItems()) {
            TipoGarrafaStock tipo = obtenerTipoGarrafa(item.getTipoGarrafaId());
            EstadoGarrafa estado = obtenerEstadoGarrafa(item.getEstadoGarrafaId());
            validarActivos(camion, central, tipo, estado);

            movimientoStockService.decrementarStock(camion, tipo, estado, item.getCantidad());
            movimientoStockService.incrementarStock(central, tipo, estado, item.getCantidad());

            MovimientoGarrafa mov = MovimientoGarrafa.builder()
                    .tipoMovimiento(TipoMovimiento.DESCARGA_CAMION)
                    .depositoOrigen(camion)
                    .depositoDestino(central)
                    .tipoGarrafa(tipo)
                    .estadoOrigen(estado)
                    .estadoDestino(estado)
                    .cantidad(item.getCantidad())
                    .usuario(usuario)
                    .observaciones(req.getObservaciones())
                    .build();

            respuestas.add(movimientoMapper.toResponse(movimientoStockService.registrarMovimiento(mov)));
        }
        return respuestas;
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

    private void validarActivos(Deposito o, Deposito d, TipoGarrafaStock t, EstadoGarrafa e) {
        if (!o.isActivo()) throw new com.sistemagas.pedidos.exception.DepositoInactivoException("El depósito origen está inactivo: " + o.getNombre());
        if (!d.isActivo()) throw new com.sistemagas.pedidos.exception.DepositoInactivoException("El depósito destino está inactivo: " + d.getNombre());
        if (!t.isActivo()) throw new BusinessException("El tipo de garrafa está inactivo: " + t.getCodigo());
        if (!e.isActivo()) throw new BusinessException("El estado de garrafa está inactivo: " + e.getCodigo());
    }
}
