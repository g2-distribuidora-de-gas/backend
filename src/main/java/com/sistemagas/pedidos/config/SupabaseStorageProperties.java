package com.sistemagas.pedidos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.supabase.storage")
public class SupabaseStorageProperties {

    private boolean enabled = false;
    private String projectRef;
    private String serviceRoleKey;
    private String bucket = "pedidos-evidencia";
    private String publicBaseUrl;
    private long maxFileSizeBytes = 10L * 1024L * 1024L;
    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;

    public String getBaseUrl() {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl;
        }
        if (projectRef == null || projectRef.isBlank()) {
            return null;
        }
        return "https://" + projectRef + ".supabase.co/storage/v1";
    }

    public String getPublicUrl(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }
        String base = getBaseUrl();
        if (base == null) {
            return null;
        }
        String cleanPath = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
        return base + "/object/public/" + bucket + "/" + cleanPath;
    }
}