package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResumenRepartidor {
    private Long id;
    private String nombre;
    private String email;
}