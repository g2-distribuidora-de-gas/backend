package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.response.EstadoGarrafaResponse;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstadoGarrafaMapper {

    EstadoGarrafaResponse toResponse(EstadoGarrafa entity);
}
