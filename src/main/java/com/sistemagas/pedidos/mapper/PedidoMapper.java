package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PedidoDetalleMapper.class})
public interface PedidoMapper {

    @Mapping(source = "usuarioId", target = "usuarioId")
    @Mapping(target = "usuarioNombreCompleto", ignore = true)
    @Mapping(target = "total", expression = "java(pedido.getDetalles() == null ? java.math.BigDecimal.ZERO : pedido.getDetalles().stream().map(d -> d.getSubtotal() == null ? java.math.BigDecimal.ZERO : d.getSubtotal()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))")
    PedidoResponse toResponse(Pedido pedido);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    Pedido toEntity(PedidoRequest request);

    default void fillUsuarioNombreCompleto(PedidoResponse response, Pedido pedido, Usuario usuario) {
        if (usuario != null) {
            response.setUsuarioNombreCompleto(usuario.getNombre() + " " + usuario.getApellido());
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    void updateEntityFromRequest(PedidoRequest request, @MappingTarget Pedido pedido);
}