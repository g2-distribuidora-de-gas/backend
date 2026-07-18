package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.response.PedidoDetalleResponse;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.PedidoDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PedidoDetalleMapper {

    @Mapping(target = "garrafaTipo", ignore = true)
    PedidoDetalleResponse toResponse(PedidoDetalle detalle);

    default void applyGarrafaTipo(PedidoDetalleResponse response, TipoGarrafaStock garrafa) {
        if (garrafa != null) {
            response.setGarrafaTipo(garrafa.getCodigo());
        }
    }
}