package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;
import com.sistemagas.pedidos.model.Usuario;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    Usuario toEntity(UsuarioRequest request);

    UsuarioResponse toResponse(Usuario usuario);

    void updateEntity(@MappingTarget Usuario usuario, UsuarioRequest request);

    @AfterMapping
    default void fillNombreCompleto(Usuario usuario, @MappingTarget UsuarioResponse.UsuarioResponseBuilder responseBuilder) {
        if (usuario != null) {
            String nombreCompleto = String.format("%s %s", 
                usuario.getNombre() != null ? usuario.getNombre() : "", 
                usuario.getApellido() != null ? usuario.getApellido() : "").trim();
            responseBuilder.nombreCompleto(nombreCompleto);
        }
    }
}
