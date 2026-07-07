package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.ClienteRequest;
import com.sistemagas.pedidos.dto.response.ClienteResponse;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .build();
        
        Cliente creado = clienteService.crearCliente(cliente);
        return new ResponseEntity<>(mapToResponse(creado), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        Cliente clienteModificado = Cliente.builder()
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .build();
                
        Cliente actualizado = clienteService.actualizarCliente(id, clienteModificado);
        return ResponseEntity.ok(mapToResponse(actualizado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Long id) {
        Cliente cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(mapToResponse(cliente));
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarTodos() {
        List<ClienteResponse> lista = clienteService.listarTodos()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    private ClienteResponse mapToResponse(Cliente cliente) {
        return ClienteResponse.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .latitud(cliente.getLatitud())
                .longitud(cliente.getLongitud())
                .placeId(cliente.getPlaceId())
                .geoActualizadoEn(cliente.getGeoActualizadoEn())
                .build();
    }
}
