package com.sistemagas.pedidos.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class SupabaseWebClientConfig {

    private final SupabaseStorageProperties properties;

    public SupabaseWebClientConfig(SupabaseStorageProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "supabaseStorageWebClient")
    @ConditionalOnProperty(name = "app.supabase.storage.enabled", havingValue = "true")
    public WebClient supabaseStorageWebClient() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "Supabase Storage baseUrl no configurado. Define app.supabase.storage.project-ref "
                            + "o app.supabase.storage.public-base-url.");
        }

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl);

        if (properties.getServiceRoleKey() != null && !properties.getServiceRoleKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getServiceRoleKey());
        }

        return builder.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}