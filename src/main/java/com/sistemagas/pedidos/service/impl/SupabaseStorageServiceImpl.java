package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.SupabaseStorageProperties;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    private final WebClient webClient;
    private final SupabaseStorageProperties properties;

    public SupabaseStorageServiceImpl(@Qualifier("supabaseStorageWebClient") @Lazy WebClient webClient,
                                      SupabaseStorageProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public String subir(String prefijoPath, MultipartFile archivo, String descripcion) {
        validarStorageHabilitado();
        validarArchivo(archivo);

        byte[] contenido = leerBytes(archivo);
        String contentType = archivo.getContentType();
        String extension = extensionFromContentType(contentType);
        String objectPath = prefijoPath + "/" + UUID.randomUUID() + extension;

        subirBytes(objectPath, contenido, contentType);
        log.info("Archivo subido a Supabase Storage: objectPath={}, descripcion={}", objectPath, descripcion);
        return objectPath;
    }

    @Override
    @Deprecated
    public String subir(Long pedidoId, MultipartFile archivo, String descripcion) {
        return subir("pedido-" + pedidoId, archivo, descripcion);
    }

    @Override
    public void eliminar(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }
        try {
            webClient.delete()
                    .uri("/object/{bucket}/{path}", properties.getBucket(), objectPath)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Archivo eliminado de Supabase Storage: {}", objectPath);
        } catch (Exception ex) {
            log.warn("Error eliminando archivo de Supabase Storage (compensacion): {}", ex.getMessage());
        }
    }

    @Override
    public String getSignedUrl(String objectPath) {
        return getSignedUrl(objectPath, properties.getSignedUrlTtlSeconds());
    }

    @Override
    public String getSignedUrl(String objectPath, int ttlSeconds) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }
        if (!properties.isEnabled()) {
            log.warn("getSignedUrl llamado pero Supabase Storage esta deshabilitado. objectPath={}", objectPath);
            return null;
        }
        try {
            Map<String, Object> body = Map.of("expiresIn", ttlSeconds);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/object/sign/{bucket}/{path}", properties.getBucket(), objectPath)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(b -> new BusinessException(
                                            "Error generando signed URL de Supabase Storage ("
                                                    + resp.statusCode().value() + "): " + b)))
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new BusinessException("Respuesta vacia de Supabase Storage al generar signed URL");
            }
            String signedUrl = (String) response.get("signedURL");
            if (signedUrl == null || signedUrl.isBlank()) {
                throw new BusinessException("Supabase Storage no devolvio signedURL en la respuesta");
            }
            return signedUrl;
        } catch (BusinessException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            log.error("Error HTTP de Supabase Storage al generar signed URL: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new BusinessException("Error generando signed URL: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error inesperado generando signed URL de Supabase Storage", ex);
            throw new BusinessException("Error generando signed URL: " + ex.getMessage());
        }
    }

    @Override
    public void mover(String sourcePath, String destPath) {
        if (sourcePath == null || sourcePath.isBlank()
                || destPath == null || destPath.isBlank()) {
            throw new BusinessException("sourcePath y destPath son obligatorios para mover");
        }
        try {
            Map<String, String> body = Map.of(
                    "bucketId", properties.getBucket(),
                    "sourceKey", sourcePath,
                    "destinationKey", destPath
            );
            webClient.post()
                    .uri("/object/move")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(b -> new BusinessException(
                                            "Error moviendo archivo en Supabase Storage ("
                                                    + resp.statusCode().value() + "): " + b)))
                    .toBodilessEntity()
                    .block();
            log.info("Archivo movido en Supabase Storage: {} -> {}", sourcePath, destPath);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado moviendo archivo en Supabase Storage: {} -> {}",
                    sourcePath, destPath, ex);
            throw new BusinessException("Error moviendo archivo en Supabase Storage: " + ex.getMessage());
        }
    }

    private void validarStorageHabilitado() {
        if (!properties.isEnabled()) {
            throw new BusinessException(
                    "Supabase Storage esta deshabilitado. Habilitalo en app.supabase.storage.enabled=true.");
        }
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("El archivo es obligatorio");
        }
        String contentType = archivo.getContentType();
        if (contentType == null
                || !properties.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    "Tipo de archivo no permitido. Permitidos: " + properties.getAllowedContentTypes());
        }
        if (archivo.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BusinessException(
                    "El archivo excede el tamano maximo permitido ("
                            + (properties.getMaxFileSizeBytes() / 1024L / 1024L) + " MB).");
        }
    }

    private byte[] leerBytes(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    private void subirBytes(String objectPath, byte[] contenido, String contentType) {
        try {
            webClient.post()
                    .uri("/object/{bucket}/{path}", properties.getBucket(), objectPath)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .bodyValue(contenido)
                    .retrieve()
                    .onStatus(s -> s.isError(), resp ->
                            resp.bodyToMono(String.class).defaultIfEmpty("")
                                    .map(body -> new BusinessException(
                                            "Error subiendo archivo a Supabase Storage ("
                                                    + resp.statusCode().value() + "): " + body)))
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Error HTTP de Supabase Storage: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new BusinessException("Error de Supabase Storage: " + ex.getMessage());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error inesperado subiendo archivo a Supabase Storage", ex);
            throw new BusinessException("Error subiendo archivo a Supabase Storage: " + ex.getMessage());
        }
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".bin";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }
}
