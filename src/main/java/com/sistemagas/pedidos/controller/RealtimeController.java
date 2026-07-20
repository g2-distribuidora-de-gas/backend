package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.realtime.ErrorWsDto;
import com.sistemagas.pedidos.dto.realtime.PosicionRepartidorDto;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.security.WebSocketAuthInterceptor;
import com.sistemagas.pedidos.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RealtimeController {

    private final TrackingService trackingService;
    private final UsuarioRepository usuarioRepository;

    @MessageMapping("/rutas/{rutaId}/posicion")
    public void onPosicion(@DestinationVariable Long rutaId,
                           @Payload PosicionRepartidorDto dto,
                           SimpMessageHeaderAccessor headers) {
        Map<String, Object> sessionAttrs = headers.getSessionAttributes();
        if (sessionAttrs == null || sessionAttrs.get(WebSocketAuthInterceptor.SESSION_EMAIL) == null) {
            throw new BusinessException("Sesion WS sin identidad",
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "NO_AUTH");
        }
        String email = (String) sessionAttrs.get(WebSocketAuthInterceptor.SESSION_EMAIL);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(
                        "Usuario autenticado no encontrado",
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "USUARIO_NO_ENCONTRADO"));

        dto.setRutaId(rutaId);
        trackingService.publicarPosicion(usuario, dto);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser(destinations = "/queue/errors")
    public ErrorWsDto handleBusiness(BusinessException ex) {
        log.warn("WS BusinessException: {} ({})", ex.getMessage(), ex.getCodigo());
        return ErrorWsDto.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(Instant.now())
                .build();
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(destinations = "/queue/errors")
    public ErrorWsDto handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("WS IllegalArgumentException: {}", ex.getMessage());
        return ErrorWsDto.builder()
                .codigo("BAD_REQUEST")
                .mensaje(ex.getMessage())
                .timestamp(Instant.now())
                .build();
    }
}
