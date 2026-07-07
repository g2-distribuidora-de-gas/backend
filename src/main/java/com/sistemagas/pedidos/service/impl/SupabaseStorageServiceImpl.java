package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.SupabaseStorageProperties;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    private final WebClient webClient;
    private final SupabaseStorageProperties properties;

    public SupabaseStorageServiceImpl(@Qualifier("supabaseStorageWebClient") WebClient webClient,
                                      SupabaseStorageProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public String subir(Long pedidoId, MultipartFile archivo, String descripcion) {
        if (!properties.isEnabled()) {
            throw new BusinessException(
                    "Supabase Storage esta deshabilitado. Habilitalo en app.supabase.storage.enabled=true.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("El archivo es obligatorio");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !properties.getAllowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(
                    "Tipo de archivo no permitido. Permitidos: " + properties.getAllowedContentTypes());
        }

        if (archivo.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BusinessException(
                    "El archivo excede el tamano maximo permitido ("
                            + (properties.getMaxFileSizeBytes() / 1024L / 1024L) + " MB).");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo leer el archivo: " + ex.getMessage());
        }

        String extension = extensionFromContentType(contentType);
        String objectPath = "pedido-" + pedidoId + "/" + UUID.randomUUID() + extension;

        try {
            HttpStatusCode status = webClient.post()
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
                    .map(entity -> entity.getStatusCode())
                    .block();

            if (status == null || !status.is2xxSuccessful()) {
                throw new BusinessException("Supabase Storage respondio con estado " + status);
            }
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

        String urlPublica = properties.getPublicUrl(objectPath);
        if (urlPublica == null) {
            throw new BusinessException("No se pudo construir la URL publica del archivo.");
        }

        log.info("Foto de evidencia subida a Supabase Storage: pedidoId={}, objectPath={}, descripcion={}",
                pedidoId, objectPath, descripcion);

        return urlPublica;
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