package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.request.ClienteRequest;
import com.sistemagas.pedidos.dto.response.ApiResponse;
import com.sistemagas.pedidos.dto.response.ClienteFotoResponse;
import com.sistemagas.pedidos.dto.response.ClienteResponse;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.service.ClienteFotoService;
import com.sistemagas.pedidos.service.ClienteService;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "API para la gestion de clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteFotoService clienteFotoService;
    private final SupabaseStorageService supabaseStorageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Crear un cliente",
            description = "Registra un nuevo cliente con sus datos basicos y coordenadas opcionales.")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .build();

        Cliente creado = clienteService.crearCliente(cliente);
        return new ResponseEntity<>(mapToResponse(creado), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Actualizar un cliente",
            description = "Modifica los datos de un cliente existente identificado por su ID.")
    public ResponseEntity<ClienteResponse> actualizar(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        Cliente clienteModificado = Cliente.builder()
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .build();

        Cliente actualizado = clienteService.actualizarCliente(id, clienteModificado);
        return ResponseEntity.ok(mapToResponse(actualizado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un cliente por su ID",
            description = "Retorna el detalle de un cliente, incluyendo urlFotoEvidencia firmada si tiene foto asociada.")
    public ResponseEntity<ClienteResponse> obtener(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long id) {
        Cliente cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(mapToResponse(cliente));
    }

    @GetMapping
    @Operation(summary = "Listar todos los clientes",
            description = "Retorna la lista completa de clientes registrados.")
    public ResponseEntity<List<ClienteResponse>> listarTodos() {
        List<ClienteResponse> lista = clienteService.listarTodos()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Eliminar un cliente",
            description = "Da de baja logica al cliente (no se elimina fisicamente).")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Subir primera foto de fachada del cliente",
            description = "Asocia la primera foto de evidencia al cliente. "
                    + "Retorna 409 Conflict si el cliente ya tiene foto (usar PUT para reemplazar).")
    public ResponseEntity<ApiResponse<ClienteFotoResponse>> subirFoto(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long id,
            @Parameter(description = "Archivo de imagen (jpg/png/webp). En el form-data el campo debe llamarse 'archivo'.")
            @RequestParam("archivo") MultipartFile archivo,
            @Parameter(description = "Descripcion opcional de la evidencia (texto libre, no se persiste en Storage)")
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        ClienteFotoResponse response = clienteFotoService.subirFoto(id, archivo, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Foto de fachada asociada al cliente"));
    }

    @PutMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PREVENTISTA', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Reemplazar foto de fachada del cliente",
            description = "Reemplaza la foto existente. La version anterior se elimina de Supabase. "
                    + "Retorna 409 Conflict si el cliente no tiene foto previa.")
    public ResponseEntity<ApiResponse<ClienteFotoResponse>> reemplazarFoto(
            @Parameter(description = "ID del cliente", example = "1") @PathVariable Long id,
            @Parameter(description = "Archivo de imagen (jpg/png/webp). En el form-data el campo debe llamarse 'archivo'.")
            @RequestParam("archivo") MultipartFile archivo,
            @Parameter(description = "Descripcion opcional de la evidencia (texto libre, no se persiste en Storage)")
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        ClienteFotoResponse response = clienteFotoService.reemplazarFoto(id, archivo, descripcion);
        return ResponseEntity.ok(ApiResponse.ok(response, "Foto de fachada reemplazada"));
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
                .geocodePrecision(cliente.getGeocodePrecision())
                .geoActualizadoEn(cliente.getGeoActualizadoEn())
                .activo(cliente.getActivo())
                .urlFotoEvidencia(
                        cliente.getFotoEvidenciaPath() != null
                                ? supabaseStorageService.getSignedUrl(cliente.getFotoEvidenciaPath())
                                : null)
                .build();
    }
}
