package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.LoginRequest;
import com.sistemagas.pedidos.dto.request.RegisterRequest;
import com.sistemagas.pedidos.dto.response.AuthResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.security.JwtTokenProvider;
import com.sistemagas.pedidos.service.AuthService;
import com.sistemagas.pedidos.util.Constantes;
import com.sistemagas.pedidos.util.RolJerarquiaHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RolJerarquiaHelper rolJerarquiaHelper;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "Credenciales inválidas",
                        HttpStatus.UNAUTHORIZED, "AUTH_FAILED"));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException(
                    "El usuario se encuentra desactivado. Contacte al administrador.",
                    HttpStatus.FORBIDDEN, "USUARIO_DESACTIVADO");
        }

        if (usuario.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new BusinessException(
                    "Credenciales inválidas",
                    HttpStatus.UNAUTHORIZED, "AUTH_FAILED");
        }

        String token = jwtTokenProvider.generateToken(usuario);
        log.info("Login exitoso: email={}, rol={}", usuario.getEmail(), usuario.getRol());

        return buildAuthResponse(token, usuario);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, Usuario creador) {
        // 1. Validar jerarquía de roles
        rolJerarquiaHelper.validarPermisoCreacion(creador.getRol(), request.getRol());

        // 2. Validar unicidad de email
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe un usuario con ese email");
        }

        // 3. Validar unicidad de DNI
        if (usuarioRepository.existsByDni(request.getDni())) {
            throw new BusinessException(Constantes.MSG_DNI_DUPLICADO);
        }

        // 4. Crear usuario
        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .dni(request.getDni())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .activo(true)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        log.info("Usuario registrado: id={}, email={}, rol={}, creadoPor={}",
                saved.getId(), saved.getEmail(), saved.getRol(), creador.getEmail());

        // 5. Generar token y responder
        String token = jwtTokenProvider.generateToken(saved);
        return buildAuthResponse(token, saved);
    }

    private AuthResponse buildAuthResponse(String token, Usuario usuario) {
        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .userId(usuario.getId())
                .email(usuario.getEmail())
                .nombreCompleto(String.format("%s %s",
                        usuario.getNombre() != null ? usuario.getNombre() : "",
                        usuario.getApellido() != null ? usuario.getApellido() : "").trim())
                .rol(usuario.getRol())
                .build();
    }
}
