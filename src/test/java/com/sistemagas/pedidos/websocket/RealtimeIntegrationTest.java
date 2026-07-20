package com.sistemagas.pedidos.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagas.pedidos.dto.realtime.PosicionBroadcastDto;
import com.sistemagas.pedidos.dto.realtime.PosicionRepartidorDto;
import com.sistemagas.pedidos.dto.realtime.WsDestinations;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.datasource.url=jdbc:h2:mem:ws-testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "app.jwt.secret=clave-secreta-de-desarrollo-cambiar-en-produccion-min-64-chars-!!!!",
                "app.jwt.expiration-ms=86400000",
                "app.supabase.storage.enabled=false",
                "locationiq.api-key=test"
        }
)
@ActiveProfiles("test")
class RealtimeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private RutaRepository rutaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("REPARTIDOR publica su posicion y ADMIN la recibe en el topico de la ruta")
    void repartidor_publica_admin_recibe() throws Exception {
        Usuario repartidor = buildUsuario("repartidor@test.com", RolUsuario.REPARTIDOR, 5L);
        Usuario admin = buildUsuario("admin@test.com", RolUsuario.ADMIN, 1L);
        Ruta ruta = buildRuta(repartidor, EstadoRuta.EN_CURSO, 10L);

        when(usuarioRepository.findByEmail("repartidor@test.com")).thenReturn(Optional.of(repartidor));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(rutaRepository.findById(10L)).thenReturn(Optional.of(ruta));

        String repartidorJwt = jwtTokenProvider.generateToken(repartidor);
        String adminJwt = jwtTokenProvider.generateToken(admin);

        BlockingQueue<PosicionBroadcastDto> recibidos = new LinkedBlockingDeque<>();
        BlockingQueue<Throwable> errores = new LinkedBlockingDeque<>();

        StompSession adminSession = connectAs(adminJwt, errores);
        adminSession.subscribe(WsDestinations.topicPosiciones(ruta.getId()),
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return PosicionBroadcastDto.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        recibidos.add((PosicionBroadcastDto) payload);
                    }
                });

        Thread.sleep(800);

        StompSession repartidorSession = connectAs(repartidorJwt, errores);
        Thread.sleep(500);

        PosicionRepartidorDto payload = PosicionRepartidorDto.builder()
                .latitud(new BigDecimal("-26.2072404"))
                .longitud(new BigDecimal("-58.2123249"))
                .headingGrados(new BigDecimal("125.5"))
                .velocidadMps(new BigDecimal("8.4"))
                .precisionM(new BigDecimal("5.0"))
                .timestampCliente(Instant.now())
                .origen("GPS")
                .build();

        repartidorSession.send(WsDestinations.appPosicion(ruta.getId()), payload);

        PosicionBroadcastDto broadcast = recibidos.poll(10, TimeUnit.SECONDS);
        Throwable error = errores.poll();

        assertThat(error)
                .as("No debe haber errores en la sesion WS")
                .isNull();
        assertThat(broadcast)
                .as("El admin debe recibir el broadcast en /topic/rutas/10/posiciones")
                .isNotNull();
        assertThat(broadcast.getRutaId()).isEqualTo(ruta.getId());
        assertThat(broadcast.getRepartidorId()).isEqualTo(repartidor.getId());
        assertThat(broadcast.getEstadoRuta()).isEqualTo(EstadoRuta.EN_CURSO);
        assertThat(broadcast.getLatitud()).isEqualByComparingTo(payload.getLatitud());
        assertThat(broadcast.getLongitud()).isEqualByComparingTo(payload.getLongitud());
        assertThat(broadcast.getServerTimestamp()).isNotNull();

        repartidorSession.disconnect();
        adminSession.disconnect();
    }

    private StompSession connectAs(String jwt, BlockingQueue<Throwable> errores) throws Exception {
        String url = String.format("ws://localhost:%d/ws-native?token=%s", port, jwt);
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();

        return stompClient.connectAsync(url, handshakeHeaders, connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable exception) {
                        errores.add(exception);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        errores.add(exception);
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    private Usuario buildUsuario(String email, RolUsuario rol, Long id) {
        return Usuario.builder()
                .id(id)
                .nombre("Test")
                .apellido("User")
                .dni("DNI-" + id)
                .email(email)
                .rol(rol)
                .activo(true)
                .passwordHash("x")
                .version(0L)
                .build();
    }

    private Ruta buildRuta(Usuario repartidor, EstadoRuta estado, Long id) {
        return Ruta.builder()
                .id(id)
                .fechaReparto(java.time.LocalDate.now())
                .repartidor(repartidor)
                .origenLat(new BigDecimal("-26.2072404"))
                .origenLng(new BigDecimal("-58.2123249"))
                .estado(estado)
                .version(0L)
                .build();
    }
}
