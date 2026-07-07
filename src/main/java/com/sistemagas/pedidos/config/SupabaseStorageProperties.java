package com.sistemagas.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProjectRef() {
        return projectRef;
    }

    public void setProjectRef(String projectRef) {
        this.projectRef = projectRef;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public void setServiceRoleKey(String serviceRoleKey) {
        this.serviceRoleKey = serviceRoleKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

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