package com.sistemagas.pedidos.websocket;

import com.sistemagas.pedidos.security.JwtTokenProvider;
import com.sistemagas.pedidos.security.WebSocketAuthInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    private static final String SECRET = "clave-secreta-de-desarrollo-cambiar-en-produccion-min-64-chars-!!!!";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private WebSocketAuthInterceptor interceptor;

    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtTokenProvider);
        testKey = io.jsonwebtoken.Jwts.SIG.HS256.key().build();
    }

    @Test
    @DisplayName("Handshake: rechaza cuando no hay token")
    void beforeHandshake_rechazaSinToken() {
        ServerHttpRequest request = servletRequest(null);
        StubServerHttpResponse response = new StubServerHttpResponse();
        Map<String, Object> attrs = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, mockWsHandler(), attrs);

        assertThat(result).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Handshake: rechaza cuando el token es invalido")
    void beforeHandshake_rechazaTokenInvalido() {
        ServerHttpRequest request = servletRequest("invalid-token");
        StubServerHttpResponse response = new StubServerHttpResponse();
        Map<String, Object> attrs = new HashMap<>();

        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        boolean result = interceptor.beforeHandshake(request, response, mockWsHandler(), attrs);

        assertThat(result).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Handshake: acepta y guarda atributos cuando el token es valido")
    void beforeHandshake_aceptaTokenValido() {
        String jwt = buildJwt("repartidor@test.com", 42L, "REPARTIDOR", "Juan Perez");
        ServerHttpRequest request = servletRequest(jwt);
        StubServerHttpResponse response = new StubServerHttpResponse();
        Map<String, Object> attrs = new HashMap<>();

        when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
        when(jwtTokenProvider.getKey()).thenReturn(testKey);

        boolean result = interceptor.beforeHandshake(request, response, mockWsHandler(), attrs);

        assertThat(result).isTrue();
        assertThat(attrs).containsEntry(WebSocketAuthInterceptor.SESSION_EMAIL, "repartidor@test.com");
        assertThat(attrs).containsEntry(WebSocketAuthInterceptor.SESSION_ROL, "REPARTIDOR");
        assertThat(attrs).containsEntry(WebSocketAuthInterceptor.SESSION_NOMBRE, "Juan Perez");
        assertThat(attrs).containsKey(WebSocketAuthInterceptor.SESSION_USER_ID);
        assertThat(((Number) attrs.get(WebSocketAuthInterceptor.SESSION_USER_ID)).longValue())
                .isEqualTo(42L);
    }

    @Test
    @DisplayName("Handshake: acepta token via Authorization Bearer en header")
    void beforeHandshake_aceptaAuthorizationBearer() {
        String jwt = buildJwt("admin@test.com", 7L, "ADMIN", "Admin User");
        ServerHttpRequest request = servletRequestViaHeader("Bearer " + jwt);
        StubServerHttpResponse response = new StubServerHttpResponse();
        Map<String, Object> attrs = new HashMap<>();

        when(jwtTokenProvider.validateToken(jwt)).thenReturn(true);
        when(jwtTokenProvider.getKey()).thenReturn(testKey);

        boolean result = interceptor.beforeHandshake(request, response, mockWsHandler(), attrs);

        assertThat(result).isTrue();
        assertThat(attrs).containsEntry(WebSocketAuthInterceptor.SESSION_EMAIL, "admin@test.com");
        assertThat(attrs).containsEntry(WebSocketAuthInterceptor.SESSION_ROL, "ADMIN");
    }

    private String buildJwt(String subject, Long userId, String rol, String nombre) {
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("rol", rol)
                .claim("nombre", nombre)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(testKey)
                .compact();
    }

    private ServerHttpRequest servletRequest(String tokenQueryParam) {
        MockHttpServletRequest servlet = new MockHttpServletRequest();
        if (tokenQueryParam != null) {
            servlet.setParameter("token", tokenQueryParam);
        }
        return new ServletServerHttpRequest(servlet);
    }

    private ServerHttpRequest servletRequestViaHeader(String authorizationHeader) {
        MockHttpServletRequest servlet = new MockHttpServletRequest();
        servlet.addHeader("Authorization", authorizationHeader);
        return new ServletServerHttpRequest(servlet);
    }

    private WebSocketHandler mockWsHandler() {
        return new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) { }
            @Override
            public void handleMessage(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) { }
            @Override
            public void handleTransportError(org.springframework.web.socket.WebSocketSession session, Throwable exception) { }
            @Override
            public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) { }
            @Override
            public boolean supportsPartialMessages() { return false; }
        };
    }

    private static class StubServerHttpResponse implements ServerHttpResponse {
        private org.springframework.http.HttpStatusCode statusCode = HttpStatus.OK;
        private final org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

        public void setStatusCode(org.springframework.http.HttpStatusCode status) {
            this.statusCode = status;
        }

        public org.springframework.http.HttpStatusCode getStatusCode() {
            return statusCode;
        }

        public org.springframework.http.HttpHeaders getHeaders() {
            return headers;
        }

        public OutputStream getBody() throws IOException {
            return OutputStream.nullOutputStream();
        }

        public void close() { }

        public void flush() throws IOException { }
    }
}
