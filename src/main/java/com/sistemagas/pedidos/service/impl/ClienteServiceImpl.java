package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.location.LocationDto;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.service.ClienteService;
import com.sistemagas.pedidos.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

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
        
        boolean direccionCambio = !clienteExistente.getDireccion().equalsIgnoreCase(clienteModificado.getDireccion());
        
        clienteExistente.setNombre(clienteModificado.getNombre());
        clienteExistente.setTelefono(clienteModificado.getTelefono());
        clienteExistente.setDireccion(clienteModificado.getDireccion());
        
        if (direccionCambio) {
            geocodificarSiEsNecesario(clienteExistente);
        }
        
        return clienteRepository.save(clienteExistente);
    }

    @Override
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    @Override
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }
    
    private void geocodificarSiEsNecesario(Cliente cliente) {
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
            // Manejar error de geocoding (por ejemplo si la API está caída).
            // Podríamos dejar el cliente sin coords temporalmente.
        }
    }
}
