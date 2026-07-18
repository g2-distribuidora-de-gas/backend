package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.TipoGarrafaStockRequest;
import com.sistemagas.pedidos.dto.response.TipoGarrafaStockResponse;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TipoGarrafaStockMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    TipoGarrafaStock toEntity(TipoGarrafaStockRequest request);

    TipoGarrafaStockResponse toResponse(TipoGarrafaStock entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    void updateEntity(@MappingTarget TipoGarrafaStock entity, TipoGarrafaStockRequest request);
}
