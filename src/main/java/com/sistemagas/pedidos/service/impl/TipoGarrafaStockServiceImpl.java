package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.TipoGarrafaStockRequest;
import com.sistemagas.pedidos.dto.response.EstadoGarrafaResponse;
import com.sistemagas.pedidos.dto.response.TipoGarrafaStockResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.EstadoGarrafaMapper;
import com.sistemagas.pedidos.mapper.TipoGarrafaStockMapper;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.repository.EstadoGarrafaRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.service.TipoGarrafaStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipoGarrafaStockServiceImpl implements TipoGarrafaStockService {

    private final TipoGarrafaStockRepository tipoGarrafaStockRepository;
    private final EstadoGarrafaRepository estadoGarrafaRepository;
    private final TipoGarrafaStockMapper tipoGarrafaStockMapper;
    private final EstadoGarrafaMapper estadoGarrafaMapper;

    @Override
    @Transactional
    public TipoGarrafaStockResponse crear(TipoGarrafaStockRequest request) {
        String codigoNorm = request.getCodigo().toUpperCase();
        if (tipoGarrafaStockRepository.existsByCodigo(codigoNorm)) {
            throw new BusinessException("Ya existe un tipo de garrafa con el código '" + codigoNorm + "'");
        }
        request.setCodigo(codigoNorm);
        TipoGarrafaStock entidad = tipoGarrafaStockMapper.toEntity(request);
        TipoGarrafaStock guardado = tipoGarrafaStockRepository.save(entidad);
        log.info("TipoGarrafaStock creado: id={}, codigo='{}'", guardado.getId(), guardado.getCodigo());
        return tipoGarrafaStockMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public TipoGarrafaStockResponse actualizar(Long id, TipoGarrafaStockRequest request) {
        TipoGarrafaStock entidad = obtenerEntidad(id);
        String codigoNorm = request.getCodigo().toUpperCase();

        // Validar unicidad de código excluyendo el propio registro
        tipoGarrafaStockRepository.findByCodigo(codigoNorm).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new BusinessException("Ya existe un tipo de garrafa con el código '" + codigoNorm + "'");
            }
        });

        request.setCodigo(codigoNorm);
        tipoGarrafaStockMapper.updateEntity(entidad, request);
        TipoGarrafaStock guardado = tipoGarrafaStockRepository.save(entidad);
        log.info("TipoGarrafaStock actualizado: id={}, codigo='{}'", guardado.getId(), guardado.getCodigo());
        return tipoGarrafaStockMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoGarrafaStockResponse obtenerPorId(Long id) {
        return tipoGarrafaStockMapper.toResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoGarrafaStockResponse> listarActivos() {
        return tipoGarrafaStockRepository.findByActivoTrue()
                .stream()
                .map(tipoGarrafaStockMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoGarrafaStockResponse> listarTodos() {
        return tipoGarrafaStockRepository.findAll()
                .stream()
                .map(tipoGarrafaStockMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TipoGarrafaStockResponse cambiarEstado(Long id, boolean activo) {
        TipoGarrafaStock entidad = obtenerEntidad(id);
        entidad.setActivo(activo);
        TipoGarrafaStock guardado = tipoGarrafaStockRepository.save(entidad);
        log.info("TipoGarrafaStock id={} → activo={}", id, activo);
        return tipoGarrafaStockMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoGarrafaResponse> listarEstados() {
        return estadoGarrafaRepository.findByActivoTrue()
                .stream()
                .map(estadoGarrafaMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    private TipoGarrafaStock obtenerEntidad(Long id) {
        return tipoGarrafaStockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de garrafa no encontrado: id=" + id));
    }
}
