package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.request.LoginRequest;
import com.sistemagas.pedidos.dto.request.RegisterRequest;
import com.sistemagas.pedidos.dto.response.AuthResponse;
import com.sistemagas.pedidos.model.Usuario;

public interface AuthService {

    /**
     * Autentica un usuario con email y contraseña.
     *
     * @param request datos de login
     * @return AuthResponse con JWT y datos del usuario
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registra un nuevo usuario en el sistema.
     * El rol del nuevo usuario se valida contra la jerarquía de permisos del creador.
     *
     * @param request datos del nuevo usuario
     * @param creador usuario autenticado que está creando
     * @return AuthResponse con JWT y datos del nuevo usuario
     */
    AuthResponse register(RegisterRequest request, Usuario creador);
}
