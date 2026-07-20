package com.sistemagas.pedidos.security;

import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsSubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_POSICIONES =
            Pattern.compile("^/topic/rutas/(\\d+)/posiciones$");

    private final RutaRepository rutaRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Matcher m = TOPIC_POSICIONES.matcher(destination);
        if (!m.matches()) {
            return message;
        }

        Long rutaId = Long.valueOf(m.group(1));
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs == null) {
            throw new IllegalArgumentException("Sesion WS sin identidad autenticada");
        }

        String rol = (String) sessionAttrs.get(WebSocketAuthInterceptor.SESSION_ROL);
        Long userId = ((Number) sessionAttrs.get(WebSocketAuthInterceptor.SESSION_USER_ID)).longValue();

        if (RolUsuario.ADMIN.name().equals(rol) || RolUsuario.SUPER_ADMIN.name().equals(rol)) {
            return message;
        }

        if (RolUsuario.REPARTIDOR.name().equals(rol)) {
            Optional<Ruta> rutaOpt = rutaRepository.findById(rutaId);
            if (rutaOpt.isEmpty()) {
                throw new IllegalArgumentException("Ruta " + rutaId + " no existe");
            }
            Ruta ruta = rutaOpt.get();
            if (!userId.equals(ruta.getRepartidor().getId())) {
                throw new IllegalArgumentException(
                        "REPARTIDOR " + userId + " no puede suscribirse a la ruta " + rutaId);
            }
            return message;
        }

        throw new IllegalArgumentException("Rol " + rol + " no autorizado para suscribirse a " + destination);
    }
}
