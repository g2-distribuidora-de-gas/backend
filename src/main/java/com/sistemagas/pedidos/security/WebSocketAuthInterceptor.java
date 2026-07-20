package com.sistemagas.pedidos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor, ChannelInterceptor {

    public static final String SESSION_USER_ID = "ws.userId";
    public static final String SESSION_EMAIL = "ws.email";
    public static final String SESSION_ROL = "ws.rol";
    public static final String SESSION_NOMBRE = "ws.nombre";

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            log.warn("WS handshake rechazado: token ausente");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WS handshake rechazado: token invalido o expirado");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Claims claims = parseClaims(token);
            attributes.put(SESSION_USER_ID, claims.get("userId", Number.class).longValue());
            attributes.put(SESSION_EMAIL, claims.getSubject());
            attributes.put(SESSION_ROL, claims.get("rol", String.class));
            attributes.put(SESSION_NOMBRE, claims.get("nombre", String.class));
            return true;
        } catch (Exception ex) {
            log.warn("WS handshake rechazado: no se pudieron extraer claims del token", ex);
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null || sessionAttributes.get(SESSION_EMAIL) == null) {
                throw new IllegalArgumentException("Sesion WS sin identidad autenticada");
            }
            String email = (String) sessionAttributes.get(SESSION_EMAIL);
            String rol = (String) sessionAttributes.get(SESSION_ROL);
            Principal principal = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol))
            );
            accessor.setUser(principal);
            log.debug("WS CONNECT autenticado para {} ({})", email, rol);
        }

        return message;
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Claims parseClaims(String token) {
        try {
            return io.jsonwebtoken.Jwts.parser()
                    .verifyWith(jwtTokenProvider.getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Token invalido: " + ex.getMessage(), ex);
        }
    }
}
