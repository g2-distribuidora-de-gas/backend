package com.sistemagas.pedidos.websocket;

import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.security.WebSocketAuthInterceptor;
import com.sistemagas.pedidos.security.WsSubscriptionAuthorizationInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WsSubscriptionAuthorizationInterceptorTest {

    @Mock
    private RutaRepository rutaRepository;

    private WsSubscriptionAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WsSubscriptionAuthorizationInterceptor(rutaRepository);
    }

    @Test
    @DisplayName("SUBSCRIBE: ADMIN pasa a cualquier ruta")
    void subscribe_adminPermitido() {
        Message<?> msg = subscribeMessage("/topic/rutas/1/posiciones", "ADMIN", 1L, null);
        Message<?> result = interceptor.preSend(msg, dummyChannel());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBE: REPARTIDOR dueno de la ruta pasa")
    void subscribe_repartidorDueno() {
        Usuario dueno = Usuario.builder().id(5L).rol(RolUsuario.REPARTIDOR).build();
        Ruta ruta = Ruta.builder().id(1L).repartidor(dueno).build();
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        Message<?> msg = subscribeMessage("/topic/rutas/1/posiciones", "REPARTIDOR", 5L, 1L);
        Message<?> result = interceptor.preSend(msg, dummyChannel());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBE: REPARTIDOR que no es dueno lanza excepcion")
    void subscribe_repartidorAjeno() {
        Usuario dueno = Usuario.builder().id(5L).rol(RolUsuario.REPARTIDOR).build();
        Ruta ruta = Ruta.builder().id(1L).repartidor(dueno).build();
        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        Message<?> msg = subscribeMessage("/topic/rutas/1/posiciones", "REPARTIDOR", 99L, 1L);

        assertThatThrownBy(() -> interceptor.preSend(msg, dummyChannel()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99")
                .hasMessageContaining("ruta 1");
    }

    @Test
    @DisplayName("SUBSCRIBE: REPARTIDOR a una ruta inexistente lanza excepcion")
    void subscribe_rutaInexistente() {
        when(rutaRepository.findById(99L)).thenReturn(Optional.empty());

        Message<?> msg = subscribeMessage("/topic/rutas/99/posiciones", "REPARTIDOR", 5L, 99L);

        assertThatThrownBy(() -> interceptor.preSend(msg, dummyChannel()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("SUBSCRIBE: PREVENTISTA no autorizado")
    void subscribe_preventistaRechazado() {
        Message<?> msg = subscribeMessage("/topic/rutas/1/posiciones", "PREVENTISTA", 5L, null);

        assertThatThrownBy(() -> interceptor.preSend(msg, dummyChannel()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PREVENTISTA");
    }

    @Test
    @DisplayName("SUBSCRIBE: destinos que no son /topic/rutas/{id}/posiciones pasan sin validar rol")
    void subscribe_destinoEventoPasa() {
        Message<?> msg = subscribeMessage("/topic/rutas/1/eventos", "PREVENTISTA", 5L, null);
        Message<?> result = interceptor.preSend(msg, dummyChannel());
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("No-SUBSCRIBE: cualquier mensaje que no sea SUBSCRIBE pasa")
    void noEsSubscribe_pasa() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionAttributes(new HashMap<>());
        Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(msg, dummyChannel());
        assertThat(result).isNotNull();
    }

    private Message<?> subscribeMessage(String destination, String rol, Long userId, Long rutaId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put(WebSocketAuthInterceptor.SESSION_EMAIL, "u@test.com");
        sessionAttrs.put(WebSocketAuthInterceptor.SESSION_ROL, rol);
        sessionAttrs.put(WebSocketAuthInterceptor.SESSION_USER_ID, userId);
        accessor.setSessionAttributes(sessionAttrs);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private MessageChannel dummyChannel() {
        return new MessageChannel() {
            @Override
            public boolean send(Message<?> message) { return true; }
            @Override
            public boolean send(Message<?> message, long timeout) { return true; }
        };
    }
}
