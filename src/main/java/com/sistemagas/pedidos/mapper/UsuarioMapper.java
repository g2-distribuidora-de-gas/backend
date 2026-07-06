package com.sistemagas.pedidos.mapper;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;
import com.sistemagas.pedidos.model.Usuario;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    @Mapping(target = "nombreCompleto", ignore = true)
    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
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