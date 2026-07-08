package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.response.PedidoResponse;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.Objects;

@Mapper(
        componentModel = "spring",
        uses = {PedidoDetalleMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PedidoMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.nombre", target = "clienteNombre")
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "total", expression = "java(calcularTotal(pedido))")
    PedidoResponse toResponse(Pedido pedido);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Pedido toEntity(PedidoRequest request);

    default BigDecimal calcularTotal(Pedido pedido) {
        if (pedido == null || pedido.getDetalles() == null) {
            return BigDecimal.ZERO;
        }
        return pedido.getDetalles().stream()
                .map(PedidoDetalle::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}