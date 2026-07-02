package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.response.PedidoDetalleResponse;
import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.model.PedidoDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PedidoDetalleMapper {

    @Mapping(source = "garrafaId", target = "garrafaId")
    @Mapping(target = "garrafaTipo", ignore = true)
    PedidoDetalleResponse toResponse(PedidoDetalle detalle);

    default void fillGarrafaTipo(PedidoDetalleResponse response, Garrafa garrafa) {
        if (garrafa != null) {
            TipoGarrafa tipo = garrafa.getTipo();
            if (tipo != null) {
                response.setGarrafaTipo(tipo);
            }
        }
    }
}