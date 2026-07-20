package com.sistemagas.pedidos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.websocket")
@Getter
@Setter
public class WebSocketProperties {

    private long heartbeatMs = 10000L;

    private List<String> allowedOrigins = List.of("http://localhost:*", "http://127.0.0.1:*");

    private Posicion posicion = new Posicion();

    @Getter
    @Setter
    public static class Posicion {

        private long maxRetrasoSegundos = 300L;

        private long maxAnticipoSegundos = 60L;

        private long minIntervaloSegundos = 1L;
    }
}
