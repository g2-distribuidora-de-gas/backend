package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.GarrafaRequest;
import com.sistemagas.pedidos.dto.response.GarrafaResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.mapper.GarrafaMapper;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.repository.GarrafaRepository;
import com.sistemagas.pedidos.service.GarrafaService;
import com.sistemagas.pedidos.util.Constantes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GarrafaServiceImpl implements GarrafaService {

    private final GarrafaRepository garrafaRepository;
    private final GarrafaMapper garrafaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GarrafaResponse> listarTodas() {
        return garrafaRepository.findAll().stream()
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
        
        return garrafaMapper.toResponse(savedGarrafa);
    }
}
