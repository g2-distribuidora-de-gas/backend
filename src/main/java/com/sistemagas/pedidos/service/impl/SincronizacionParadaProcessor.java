package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.SincronizacionParadaItemRequest;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.service.RutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SincronizacionParadaProcessor {

    private final RutaService rutaService;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesarParada(SincronizacionParadaItemRequest item, String emailAutenticado) {
        Usuario autenticado = usuarioRepository.findByEmail(emailAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        rutaService.actualizarEstadoParada(item.getRutaPedidoId(), item, autenticado);
    }
}
