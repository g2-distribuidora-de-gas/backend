package com.sistemagas.pedidos.service;

public interface MessagePublisher {

    /**
     * Publica un payload en un destino STOMP broadcast.
     * Encapsula SimpMessagingTemplate para poder mockear en tests.
     */
    void publish(String destination, Object payload);

    /**
     * Publica un payload en la cola privada de un usuario específico.
     * Equivale a SimpMessagingTemplate.convertAndSendToUser(user, destination, payload).
     *
     * @param user        identificador del usuario (usualmente el email)
     * @param destination destino relativo, ej: "/queue/agenda"
     * @param payload     objeto a serializar como JSON
     */
    void publishToUser(String user, String destination, Object payload);
}
