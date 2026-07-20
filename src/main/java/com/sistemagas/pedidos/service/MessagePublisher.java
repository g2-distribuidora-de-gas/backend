package com.sistemagas.pedidos.service;

public interface MessagePublisher {

    /**
     * Publica un payload en un destino STOMP. Encapsula SimpMessagingTemplate para
     * poder mockear en tests sin chocar con las restricciones del inline mock maker
     * en versiones recientes del JDK.
     */
    void publish(String destination, Object payload);
}
