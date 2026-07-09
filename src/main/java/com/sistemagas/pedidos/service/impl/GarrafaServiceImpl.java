package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.GarrafaRequest;
import com.sistemagas.pedidos.dto.response.GarrafaResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.GarrafaMapper;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.repository.GarrafaRepository;
import com.sistemagas.pedidos.service.GarrafaService;
import com.sistemagas.pedidos.util.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarrafaServiceImpl implements GarrafaService {

    private final GarrafaRepository garrafaRepository;
    private final GarrafaMapper garrafaMapper;

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public List<GarrafaResponse> listarTodas(Instant minUpdatedAt, Integer limit) {
        int pageLimit = (limit != null && limit > 0) ? limit : 100;
        Pageable pageable = PageRequest.of(0, pageLimit, Sort.by("updatedAt").ascending().and(Sort.by("id").ascending()));
        
        List<Garrafa> garrafas = (minUpdatedAt != null)
                ? garrafaRepository.findByUpdatedAtGreaterThan(minUpdatedAt, pageable)
                : garrafaRepository.findAllBy(pageable);

        return garrafas.stream()
                .map(garrafaMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GarrafaResponse crear(GarrafaRequest request) {
        if (garrafaRepository.existsByTipo(request.getTipo())) {
            throw new BusinessException(Constantes.MSG_TIPO_GARRAFA_DUPLICADO);
        }

        Garrafa garrafa = garrafaMapper.toEntity(request);
        Garrafa savedGarrafa = garrafaRepository.save(garrafa);
        log.info("Garrafa creada: id={}, tipo={}, stock={}",
                savedGarrafa.getId(), savedGarrafa.getTipo(), savedGarrafa.getStockDisponible());

        return garrafaMapper.toResponse(savedGarrafa);
    }

@Override
    @Transactional
    public GarrafaResponse actualizar(Long id, GarrafaRequest request) {
        Garrafa garrafa = garrafaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Garrafa no encontrada"));

        if (!Objects.equals(garrafa.getTipo(), request.getTipo()) && garrafaRepository.existsByTipo(request.getTipo())) {
            throw new BusinessException(Constantes.MSG_TIPO_GARRAFA_DUPLICADO);
        }

        garrafa.setTipo(request.getTipo());
        garrafa.setCapacidadKg(request.getCapacidadKg());
        garrafa.setPrecio(request.getPrecio());
        garrafa.setStockDisponible(request.getStockDisponible());

        if (request.getActivo() != null) {
            garrafa.setActivo(request.getActivo());
        }

        Garrafa savedGarrafa = garrafaRepository.save(garrafa);
        log.info("Garrafa actualizada: id={}, tipo={}, stock={}",
                savedGarrafa.getId(), savedGarrafa.getTipo(), savedGarrafa.getStockDisponible());

        return garrafaMapper.toResponse(savedGarrafa);
    }

    @Override
    @Transactional
    public GarrafaResponse actualizarPrecio(Long id, BigDecimal precio) {
        Garrafa garrafa = garrafaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Constantes.MSG_GARRAFA_NO_ENCONTRADA + ": id=" + id));

        if (precio == null || precio.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException("El precio debe ser mayor o igual a 0.01");
        }

        BigDecimal precioAnterior = garrafa.getPrecio();
        garrafa.setPrecio(precio);
        Garrafa savedGarrafa = garrafaRepository.save(garrafa);

        log.info("Precio actualizado: id={}, anterior={}, nuevo={}",
                savedGarrafa.getId(), precioAnterior, precio);

        return garrafaMapper.toResponse(savedGarrafa);
    }

    @Override
    @Transactional
    public GarrafaResponse reponerStock(Long id, Integer cantidad, String motivo) {
        Garrafa garrafa = garrafaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        Constantes.MSG_GARRAFA_NO_ENCONTRADA + ": id=" + id));

        if (cantidad == null || cantidad < 1) {
            throw new BusinessException("La cantidad a reponer debe ser mayor o igual a 1");
        }

        Integer stockActual = garrafa.getStockDisponible();
        Integer stockAnterior = stockActual == null ? 0 : stockActual;
        Integer stockResultante = stockAnterior + cantidad;

        garrafa.setStockDisponible(stockResultante);
        Garrafa savedGarrafa = garrafaRepository.save(garrafa);

        log.info("Stock repuesto: id={}, anterior={}, incremento={}, nuevo={}, motivo={}",
                savedGarrafa.getId(), stockAnterior, cantidad, stockResultante,
                motivo == null ? "(sin motivo)" : motivo);

        return garrafaMapper.toResponse(savedGarrafa);
    }
}