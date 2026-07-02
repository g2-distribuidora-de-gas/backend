package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.GarrafaRequest;
import com.sistemagas.pedidos.dto.response.GarrafaResponse;
import com.sistemagas.pedidos.model.Garrafa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface GarrafaMapper {

    @Mapping(target = "id", ignore = true)
    Garrafa toEntity(GarrafaRequest request);

    GarrafaResponse toResponse(Garrafa garrafa);

    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget Garrafa garrafa, GarrafaRequest request);
}