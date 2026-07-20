package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.WebSocketProperties;
import com.sistemagas.pedidos.dto.realtime.EventoRutaWsDto;
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
import com.sistemagas.pedidos.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final MessagePublisher messagePublisher;
    private final RutaRepository rutaRepository;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void publicarPosicion(Usuario repartidor, PosicionRepartidorDto dto) {
        if (repartidor == null || !RolUsuario.REPARTIDOR.name().equals(repartidor.getRol().name())) {
            throw new BusinessException(
                    "Solo usuarios con rol REPARTIDOR pueden publicar posiciones",
                    HttpStatus.FORBIDDEN, "FORBIDDEN_NOT_REPARTIDOR");
        }
        if (dto == null) {
            throw new BusinessException("Payload vacio", HttpStatus.BAD_REQUEST, "PAYLOAD_VACIO");
        }
        if (dto.getRutaId() == null) {
            throw new BusinessException("rutaId es obligatorio",
                    HttpStatus.BAD_REQUEST, "RUTA_ID_REQUERIDO");
        }
        if (dto.getLatitud() == null || dto.getLongitud() == null) {
            throw new BusinessException("latitud y longitud son obligatorias",
                    HttpStatus.BAD_REQUEST, "COORDENADAS_REQUERIDAS");
        }
        if (dto.getLatitud().compareTo(BigDecimal.valueOf(-90)) < 0
                || dto.getLatitud().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BusinessException("latitud fuera de rango [-90, 90]",
                    HttpStatus.BAD_REQUEST, "LATITUD_INVALIDA");
        }
        if (dto.getLongitud().compareTo(BigDecimal.valueOf(-180)) < 0
                || dto.getLongitud().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException("longitud fuera de rango [-180, 180]",
                    HttpStatus.BAD_REQUEST, "LONGITUD_INVALIDA");
        }
        if (dto.getTimestampCliente() != null) {
            Duration delta = Duration.between(dto.getTimestampCliente(), Instant.now());
            long retrasoSegundos = delta.getSeconds();
            if (retrasoSegundos > webSocketProperties.getPosicion().getMaxRetrasoSegundos()) {
                throw new BusinessException(
                        "timestampCliente es demasiado viejo (>" +
                                webSocketProperties.getPosicion().getMaxRetrasoSegundos() + "s)",
                        HttpStatus.BAD_REQUEST, "TIMESTAMP_MUY_VIEJO");
            }
            if (-retrasoSegundos > webSocketProperties.getPosicion().getMaxAnticipoSegundos()) {
                throw new BusinessException(
                        "timestampCliente viene del futuro (+" +
                                webSocketProperties.getPosicion().getMaxAnticipoSegundos() + "s)",
                        HttpStatus.BAD_REQUEST, "TIMESTAMP_MUY_FUTURO");
            }
        }

        Ruta ruta = rutaRepository.findById(dto.getRutaId())
                .orElseThrow(() -> new BusinessException(
                        "Ruta " + dto.getRutaId() + " no existe",
                        HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA"));

        if (!ruta.getRepartidor().getId().equals(repartidor.getId())) {
            throw new BusinessException(
                    "La ruta " + dto.getRutaId() + " pertenece a otro repartidor",
                    HttpStatus.FORBIDDEN, "RUTA_NO_PROPIA");
        }

        EstadoRuta estado = ruta.getEstado();
        boolean transmite = estado == EstadoRuta.EN_CURSO || estado == EstadoRuta.PLANIFICADA;
        if (!transmite) {
            throw new BusinessException(
                    "La ruta " + dto.getRutaId() + " esta " + estado + " y no acepta posiciones",
                    HttpStatus.CONFLICT, "RUTA_NO_TRANSMITE");
        }

        PosicionBroadcastDto broadcast = PosicionBroadcastDto.builder()
                .rutaId(ruta.getId())
                .repartidorId(repartidor.getId())
                .repartidorNombre(buildNombreCompleto(repartidor))
                .estadoRuta(estado)
                .latitud(dto.getLatitud())
                .longitud(dto.getLongitud())
                .headingGrados(dto.getHeadingGrados())
                .velocidadMps(dto.getVelocidadMps())
                .precisionM(dto.getPrecisionM())
                .timestampCliente(dto.getTimestampCliente())
                .serverTimestamp(Instant.now())
                .origen(dto.getOrigen())
                .build();

        messagePublisher.publish(
                WsDestinations.topicPosiciones(ruta.getId()),
                broadcast);

        log.debug("Posicion retransmitida para ruta {} (repartidor {})",
                ruta.getId(), repartidor.getId());
    }

    @Override
    public void emitirEventoRuta(Ruta ruta, EstadoRuta estadoAnterior, EventoRutaWsDto evento) {
        if (ruta == null || evento == null) {
            return;
        }
        if (evento.getTimestamp() == null) {
            evento.setTimestamp(Instant.now());
        }
        if (evento.getRutaId() == null) {
            evento.setRutaId(ruta.getId());
        }
        if (evento.getRepartidorId() == null && ruta.getRepartidor() != null) {
            evento.setRepartidorId(ruta.getRepartidor().getId());
        }
        messagePublisher.publish(
                WsDestinations.topicEventos(ruta.getId()),
                evento);
        log.debug("Evento de ruta {} retransmitido ({})", ruta.getId(), evento.getTipo());
    }

    private String buildNombreCompleto(Usuario u) {
        if (u == null) {
            return null;
        }
        String nombre = u.getNombre() != null ? u.getNombre() : "";
        String apellido = u.getApellido() != null ? u.getApellido() : "";
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? null : completo;
    }
}
