package com.sistemagas.pedidos.dto.realtime;

public final class WsDestinations {

    private WsDestinations() {
    }

    public static final String APP_POSICION_TEMPLATE = "/app/rutas/%d/posicion";
    public static final String TOPIC_POSICIONES_TEMPLATE = "/topic/rutas/%d/posiciones";
    public static final String TOPIC_EVENTOS_TEMPLATE = "/topic/rutas/%d/eventos";
    public static final String USER_QUEUE_ERRORS = "/queue/errors";
    public static final String USER_ERRORS_DESTINATION = "/user" + USER_QUEUE_ERRORS;

    /**
     * Cola privada del repartidor para notificaciones de agenda.
     * Se entrega via convertAndSendToUser(email, USER_QUEUE_AGENDA, payload).
     */
    public static final String USER_QUEUE_AGENDA = "/queue/agenda";

    public static String topicPosiciones(Long rutaId) {
        return String.format(TOPIC_POSICIONES_TEMPLATE, rutaId);
    }

    public static String topicEventos(Long rutaId) {
        return String.format(TOPIC_EVENTOS_TEMPLATE, rutaId);
    }

    public static String appPosicion(Long rutaId) {
        return String.format(APP_POSICION_TEMPLATE, rutaId);
    }
}
