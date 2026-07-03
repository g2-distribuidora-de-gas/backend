package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.UsuarioRequest;
import com.sistemagas.pedidos.dto.response.UsuarioResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.UsuarioMapper;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.service.UsuarioService;
import com.sistemagas.pedidos.util.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional(readOnly = true)
    @Retryable(
            retryFor = {DataAccessResourceFailureException.class, QueryTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2))
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByDni(request.getDni())) {
            throw new BusinessException(Constantes.MSG_DNI_DUPLICADO);
        }

        Usuario usuario = usuarioMapper.toEntity(request);
        Usuario savedUsuario = usuarioRepository.save(usuario);
        log.info("Usuario creado: id={}, dni={}", savedUsuario.getId(), savedUsuario.getDni());

        return usuarioMapper.toResponse(savedUsuario);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado: id={}", id);
    }

    @Override
    @Transactional
    public void reactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        
        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario ya se encuentra activo");
        }
        
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuario reactivado: id={}", id);
    }
}