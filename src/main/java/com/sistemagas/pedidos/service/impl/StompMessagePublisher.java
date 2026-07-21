package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompMessagePublisher implements MessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public void publishToUser(String user, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(user, destination, payload);
    }
}

