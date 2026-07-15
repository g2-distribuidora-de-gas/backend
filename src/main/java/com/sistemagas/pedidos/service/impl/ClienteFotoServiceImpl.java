package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.response.ClienteFotoResponse;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.ClienteFotoPendiente;
import com.sistemagas.pedidos.repository.ClienteFotoPendienteRepository;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.service.ClienteFotoService;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteFotoServiceImpl implements ClienteFotoService {

    private final ClienteRepository clienteRepository;
    private final ClienteFotoPendienteRepository clienteFotoPendienteRepository;
    private final SupabaseStorageService supabaseStorageService;

    @Override
    @Transactional
    public ClienteFotoResponse subirFoto(Long clienteId, MultipartFile archivo, String descripcion) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: id=" + clienteId));

        if (cliente.getFotoEvidenciaPath() != null) {
            throw new BusinessException("El cliente ya tiene foto de evidencia. Use PUT para reemplazarla.");
        }

        String objectPath = supabaseStorageService.subir("cliente-" + clienteId, archivo, descripcion);
        cliente.setFotoEvidenciaPath(objectPath);
        clienteRepository.save(cliente);

        log.info("Foto de fachada asociada al cliente: clienteId={}, objectPath={}", clienteId, objectPath);

        return buildResponse(clienteId, objectPath, archivo);
    }

    @Override
    @Transactional
    public ClienteFotoResponse reemplazarFoto(Long clienteId, MultipartFile archivo, String descripcion) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: id=" + clienteId));

        if (cliente.getFotoEvidenciaPath() == null) {
            throw new BusinessException("El cliente no tiene foto previa. Use POST para subir la primera.");
        }

        String pathAnterior = cliente.getFotoEvidenciaPath();
        String nuevoObjectPath = supabaseStorageService.subir("cliente-" + clienteId, archivo, descripcion);

        try {
            cliente.setFotoEvidenciaPath(nuevoObjectPath);
            clienteRepository.save(cliente);
        } catch (RuntimeException ex) {
            log.error("Fallo persistiendo la nueva foto del cliente id={}. Compensando: eliminando {}",
                    clienteId, nuevoObjectPath, ex);
            supabaseStorageService.eliminar(nuevoObjectPath);
            throw ex;
        }

        supabaseStorageService.eliminar(pathAnterior);

        log.info("Foto de fachada del cliente reemplazada: clienteId={}, vieja={}, nueva={}",
                clienteId, pathAnterior, nuevoObjectPath);

        return buildResponse(clienteId, nuevoObjectPath, archivo);
    }

    @Override
    @Transactional
    public String subirImagenPendiente(Long clienteId, MultipartFile archivo, String descripcion) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: id=" + clienteId));

        Optional<ClienteFotoPendiente> existente =
                clienteFotoPendienteRepository.findFirstByClienteIdOrderByUploadedAtDescIdDesc(clienteId);
        if (existente.isPresent()) {
            // Ya hay una pendiente: idempotencia para reintento offline del cliente.
            return existente.get().getObjectPath();
        }

        if (cliente.getFotoEvidenciaPath() != null) {
            throw new BusinessException("El cliente ya tiene foto de evidencia asociada.");
        }

        String objectPath = supabaseStorageService.subir(
                "cliente-pending/" + clienteId, archivo, descripcion);

        ClienteFotoPendiente pendiente = ClienteFotoPendiente.builder()
                .clienteId(clienteId)
                .objectPath(objectPath)
                .uploadedAt(Instant.now())
                .build();
        try {
            clienteFotoPendienteRepository.save(pendiente);
            clienteFotoPendienteRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Carrera: otro thread cliente gano el INSERT mientras subiamos a Supabase.
            // Recuperamos la fila ganadora y reportamos su path como respuesta idempotente.
            log.warn("Carrera UNIQUE en cliente_foto_pendiente para cliente {}. Recuperando pendiente ganadora.",
                    clienteId);
            return clienteFotoPendienteRepository
                    .findFirstByClienteIdOrderByUploadedAtDescIdDesc(clienteId)
                    .map(ClienteFotoPendiente::getObjectPath)
                    .orElseThrow(() -> new BusinessException(
                            "El cliente ya tiene una imagen pendiente de asociar."));
        }

        log.info("Imagen pendiente subida: clienteId={}, objectPath={}", clienteId, objectPath);
        return objectPath;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asociarImagenACliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            log.warn("asociarImagenACliente: cliente {} no existe, se omite", clienteId);
            return;
        }
        if (cliente.getFotoEvidenciaPath() != null) {
            return;
        }

        Optional<ClienteFotoPendiente> pendienteOpt =
                clienteFotoPendienteRepository.findFirstByClienteIdOrderByUploadedAtDesc(clienteId);
        if (pendienteOpt.isEmpty()) {
            return;
        }

        ClienteFotoPendiente pendiente = pendienteOpt.get();
        String sourcePath = pendiente.getObjectPath();
        String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
        String destPath = "cliente-" + clienteId + "/" + fileName;

        try {
            supabaseStorageService.mover(sourcePath, destPath);
        } catch (RuntimeException ex) {
            log.warn("Fallo moviendo imagen pendiente del cliente {} de {} a {}. Se mantiene pendiente para reintento.",
                    clienteId, sourcePath, destPath, ex);
            return;
        }

        cliente.setFotoEvidenciaPath(destPath);
        clienteRepository.save(cliente);
        clienteFotoPendienteRepository.deleteByClienteId(clienteId);

        log.info("Imagen pendiente asociada al cliente: clienteId={}, objectPath={}", clienteId, destPath);
    }

    @Override
    @Transactional
    public void limpiarHuerfanos(Instant cutoff) {
        List<ClienteFotoPendiente> huerfanos = clienteFotoPendienteRepository.findByUploadedAtBefore(cutoff);
        if (huerfanos.isEmpty()) {
            return;
        }
        log.info("Limpiando {} imagenes pendientes con uploadedAt < {}", huerfanos.size(), cutoff);
        for (ClienteFotoPendiente p : huerfanos) {
            try {
                supabaseStorageService.eliminar(p.getObjectPath());
            } catch (Exception ex) {
                log.warn("No se pudo eliminar el archivo huerfano {} (se reintentara en la proxima corrida)",
                        p.getObjectPath(), ex);
                continue;
            }
            clienteFotoPendienteRepository.delete(p);
        }
    }

    private ClienteFotoResponse buildResponse(Long clienteId, String objectPath, MultipartFile archivo) {
        String signedUrl = supabaseStorageService.getSignedUrl(objectPath);
        return ClienteFotoResponse.builder()
                .clienteId(clienteId)
                .objectPath(objectPath)
                .urlFotoEvidencia(signedUrl)
                .nombreArchivo(archivo.getOriginalFilename())
                .contentType(archivo.getContentType())
                .tamanioBytes(archivo.getSize())
                .build();
    }
}
