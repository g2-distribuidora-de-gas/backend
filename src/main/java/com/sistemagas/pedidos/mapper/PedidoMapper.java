package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.UsuarioModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PedidoDetalleMapper.class})
public interface PedidoMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.nombre", target = "clienteNombre")
    @Mapping(target = "total", expression = "java(pedido.getDetalles() == null ? java.math.BigDecimal.ZERO : pedido.getDetalles().stream().map(d -> d.getSubtotal() == null ? java.math.BigDecimal.ZERO : d.getSubtotal()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))")
    PedidoResponse toResponse(Pedido pedido);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Pedido toEntity(PedidoRequest request);



    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    void updateEntityFromRequest(PedidoRequest request, @MappingTarget Pedido pedido);
}