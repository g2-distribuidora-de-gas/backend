package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.response.MovimientoResponse;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import com.sistemagas.pedidos.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {TipoGarrafaStockMapper.class, EstadoGarrafaMapper.class})
public interface MovimientoMapper {

    @Mapping(target = "depositoOrigen", source = "depositoOrigen", qualifiedByName = "depositoToResumen")
    @Mapping(target = "depositoDestino", source = "depositoDestino", qualifiedByName = "depositoToResumen")
    @Mapping(target = "usuario", source = "usuario", qualifiedByName = "usuarioToResumen")
    @Mapping(target = "pedidoId", source = "pedido.id")
    MovimientoResponse toResponse(MovimientoGarrafa movimiento);

    @Named("depositoToResumen")
    default MovimientoResponse.DepositoResumen depositoToResumen(Deposito deposito) {
        if (deposito == null) return null;
        return MovimientoResponse.DepositoResumen.builder()
                .id(deposito.getId())
                .nombre(deposito.getNombre())
                .tipo(deposito.getTipo().name())
                .build();
    }

    @Named("usuarioToResumen")
    default MovimientoResponse.UsuarioResumen usuarioToResumen(Usuario usuario) {
        if (usuario == null) return null;
        return MovimientoResponse.UsuarioResumen.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre() + " " + usuario.getApellido())
                .email(usuario.getEmail())
                .build();
    }
}
