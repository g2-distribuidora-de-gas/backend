package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.location.LocationDto;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.service.ClienteService;
import com.sistemagas.pedidos.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final GeocodingService geocodingService;

    @Override
    @Transactional
    public Cliente crearCliente(Cliente cliente) {
        geocodificarSiEsNecesario(cliente);
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente clienteModificado) {
        Cliente clienteExistente = obtenerPorId(id);
        
        boolean direccionCambio = !Objects.equals(clienteExistente.getDireccion(), clienteModificado.getDireccion());
        
        clienteExistente.setNombre(clienteModificado.getNombre());
        clienteExistente.setApellido(clienteModificado.getApellido());
        clienteExistente.setTelefono(clienteModificado.getTelefono());
        clienteExistente.setEmail(clienteModificado.getEmail());
        clienteExistente.setDni(clienteModificado.getDni());
        clienteExistente.setDireccion(clienteModificado.getDireccion());
        
        boolean coordsManuales = false;
        if (clienteModificado.getLatitud() != null && clienteModificado.getLongitud() != null) {
            clienteExistente.setLatitud(clienteModificado.getLatitud());
            clienteExistente.setLongitud(clienteModificado.getLongitud());
            clienteExistente.setGeoActualizadoEn(OffsetDateTime.now());
            clienteExistente.setGeocodePrecision("manual");
            coordsManuales = true;
        }
        
        if (direccionCambio && !coordsManuales) {
            geocodificarSiEsNecesario(clienteExistente);
        }
        
        return clienteRepository.save(clienteExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    @Override
    public List<Cliente> listarTodos() {
        return clienteRepository.findByActivoTrue();
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = obtenerPorId(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }
    
    private void geocodificarSiEsNecesario(Cliente cliente) {
        if (cliente.getLatitud() != null && cliente.getLongitud() != null) {
            cliente.setGeoActualizadoEn(OffsetDateTime.now());
            cliente.setGeocodePrecision("manual");
            return;
        }
        try {
            LocationDto loc = geocodingService.obtenerCoordenadas(cliente.getDireccion());
            if (loc != null) {
                cliente.setLatitud(loc.getLatitud());
                cliente.setLongitud(loc.getLongitud());
                cliente.setPlaceId(loc.getPlaceId());
                cliente.setGeocodePrecision(loc.getPrecision());
                cliente.setGeoActualizadoEn(OffsetDateTime.now());
            }
        } catch (Exception e) {
            log.warn("Geocoding falló para dirección: {}, se deja cliente sin coordenadas temporalmente",
                    cliente.getDireccion(), e);
        }
    }
}
