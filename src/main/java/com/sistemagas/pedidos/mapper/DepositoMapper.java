package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.DepositoRequest;
import com.sistemagas.pedidos.dto.response.DepositoResponse;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface DepositoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "repartidor", ignore = true)
    Deposito toEntity(DepositoRequest request);

    @Mapping(target = "repartidor", source = "repartidor", qualifiedByName = "usuarioToResumen")
    DepositoResponse toResponse(Deposito deposito);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "repartidor", ignore = true)
    void updateEntity(@MappingTarget Deposito deposito, DepositoRequest request);

    @Named("usuarioToResumen")
    default DepositoResponse.RepartidorResumen usuarioToResumen(Usuario usuario) {
        if (usuario == null) return null;
        return DepositoResponse.RepartidorResumen.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre() + " " + usuario.getApellido())
                .email(usuario.getEmail())
                .build();
    }
}
