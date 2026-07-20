package com.sistemagas.pedidos.websocket;

import com.sistemagas.pedidos.config.WebSocketProperties;
import com.sistemagas.pedidos.dto.realtime.PosicionBroadcastDto;
import com.sistemagas.pedidos.dto.realtime.PosicionRepartidorDto;
import com.sistemagas.pedidos.dto.realtime.WsDestinations;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.service.MessagePublisher;
import com.sistemagas.pedidos.service.impl.TrackingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    private MessagePublisher messagePublisher;

    @Mock
    private RutaRepository rutaRepository;

    private WebSocketProperties properties;
    private TrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new WebSocketProperties();
        service = new TrackingServiceImpl(messagePublisher, rutaRepository, properties);
    }

    @Test
    @DisplayName("publicarPosicion: hace broadcast cuando la ruta esta EN_CURSO y es del repartidor")
    void publicarPosicion_broadcastExitoso() {
        Usuario repartidor = Usuario.builder().id(5L).nombre("Juan").apellido("Perez")
                .rol(RolUsuario.REPARTIDOR).build();
        Ruta ruta = Ruta.builder().id(1L).estado(EstadoRuta.EN_CURSO).repartidor(repartidor).build();
        PosicionRepartidorDto dto = PosicionRepartidorDto.builder()
                .rutaId(1L)
                .latitud(new BigDecimal("-26.2072404"))
                .longitud(new BigDecimal("-58.2123249"))
                .headingGrados(new BigDecimal("125.5"))
                .velocidadMps(new BigDecimal("8.4"))
                .precisionM(new BigDecimal("5.0"))
                .timestampCliente(Instant.now())
                .origen("GPS")
                .build();

        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        service.publicarPosicion(repartidor, dto);

        ArgumentCaptor<String> dest = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagePublisher).publish(dest.capture(), payload.capture());

        assertThat(dest.getValue()).isEqualTo(WsDestinations.topicPosiciones(1L));
        PosicionBroadcastDto broadcast = (PosicionBroadcastDto) payload.getValue();
        assertThat(broadcast.getRutaId()).isEqualTo(1L);
        assertThat(broadcast.getRepartidorId()).isEqualTo(5L);
        assertThat(broadcast.getRepartidorNombre()).isEqualTo("Juan Perez");
        assertThat(broadcast.getEstadoRuta()).isEqualTo(EstadoRuta.EN_CURSO);
        assertThat(broadcast.getLatitud()).isEqualByComparingTo(dto.getLatitud());
        assertThat(broadcast.getServerTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("publicarPosicion: lanza BusinessException si la ruta esta COMPLETADA")
    void publicarPosicion_rechazaRutaCompletada() {
        Usuario repartidor = Usuario.builder().id(5L).rol(RolUsuario.REPARTIDOR).build();
        Ruta ruta = Ruta.builder().id(1L).estado(EstadoRuta.COMPLETADA).repartidor(repartidor).build();
        PosicionRepartidorDto dto = PosicionRepartidorDto.builder()
                .rutaId(1L)
                .latitud(new BigDecimal("-26.207"))
                .longitud(new BigDecimal("-58.212"))
                .timestampCliente(Instant.now())
                .build();

        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> service.publicarPosicion(repartidor, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETADA");

        verify(messagePublisher, never()).publish(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("publicarPosicion: lanza BusinessException si la ruta pertenece a otro repartidor")
    void publicarPosicion_rechazaRutaAjena() {
        Usuario otroRepartidor = Usuario.builder().id(99L).rol(RolUsuario.REPARTIDOR).build();
        Usuario repartidor = Usuario.builder().id(5L).rol(RolUsuario.REPARTIDOR).build();
        Ruta ruta = Ruta.builder().id(1L).estado(EstadoRuta.EN_CURSO).repartidor(otroRepartidor).build();
        PosicionRepartidorDto dto = PosicionRepartidorDto.builder()
                .rutaId(1L)
                .latitud(new BigDecimal("-26.207"))
                .longitud(new BigDecimal("-58.212"))
                .timestampCliente(Instant.now())
                .build();

        when(rutaRepository.findById(1L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> service.publicarPosicion(repartidor, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("otro repartidor");

        verify(messagePublisher, never()).publish(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("publicarPosicion: rechaza cuando el usuario no es REPARTIDOR")
    void publicarPosicion_rechazaNoRepartidor() {
        Usuario admin = Usuario.builder().id(1L).rol(RolUsuario.ADMIN).build();
        PosicionRepartidorDto dto = PosicionRepartidorDto.builder()
                .rutaId(1L)
                .latitud(new BigDecimal("0"))
                .longitud(new BigDecimal("0"))
                .build();

        assertThatThrownBy(() -> service.publicarPosicion(admin, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("REPARTIDOR");

        verify(rutaRepository, never()).findById(any());
        verify(messagePublisher, never()).publish(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("publicarPosicion: rechaza latitud fuera de rango")
    void publicarPosicion_rechazaLatitudInvalida() {
        Usuario repartidor = Usuario.builder().id(5L).rol(RolUsuario.REPARTIDOR).build();
        PosicionRepartidorDto dto = PosicionRepartidorDto.builder()
                .rutaId(1L)
                .latitud(new BigDecimal("91.0"))
                .longitud(new BigDecimal("0"))
                .build();

        assertThatThrownBy(() -> service.publicarPosicion(repartidor, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("latitud");

        verify(messagePublisher, never()).publish(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("publicarPosicion: rechaza timestampCliente demasiado viejo")
    void publicarPosicion_rechazaTimestampViejo() {
        Usuario repartidor = Usuario.builder().id(5L).rol(RolUsuario.REPARTIDOR).build();
        PosicionRepartidorDto dto = PosicionRepartidorDto.builder()
                .rutaId(1L)
                .latitud(new BigDecimal("0"))
                .longitud(new BigDecimal("0"))
                .timestampCliente(Instant.now().minusSeconds(properties.getPosicion().getMaxRetrasoSegundos() + 60))
                .build();

        assertThatThrownBy(() -> service.publicarPosicion(repartidor, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("viejo");

        verify(messagePublisher, never()).publish(any(String.class), any(Object.class));
    }
}
