package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.ClienteRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionClienteResponse;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionClienteProcessor {

    private final ClienteService clienteService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SincronizacionClienteResponse.Procesado procesar(ClienteRequest request) {
        
        Cliente cliente = Cliente.builder()
                .uuidOffline(request.getUuidOffline())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .dni(request.getDni())
                .direccion(request.getDireccion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .build();

        Cliente creado = clienteService.crearCliente(cliente);

        log.info("Cliente sincronizado: id={}, uuidOffline={}",
                creado.getId(), request.getUuidOffline());

        return SincronizacionClienteResponse.Procesado.builder()
                .uuidOffline(request.getUuidOffline())
                .clienteId(creado.getId())
                .build();
    }
}
