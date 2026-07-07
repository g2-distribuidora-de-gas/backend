package com.sistemagas.pedidos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupabaseConfigValidator implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "supabaseConfigValidation";
    private static final String PROFILES_PROPERTY = "spring.profiles.active";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean esProduccion = false;
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                esProduccion = true;
                break;
            }
        }
        if (!esProduccion) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        List<String> errores = new ArrayList<>();

        String projectRef = environment.getProperty("SUPABASE_PROJECT_REF");
        String password = environment.getProperty("SUPABASE_PASS");
        String dbUser = environment.getProperty("SUPABASE_DB_USER");
        String dbUrl = environment.getProperty("SUPABASE_DB_URL");

        if (projectRef == null || projectRef.isBlank()
                || "undefined".equals(projectRef)
                || "tu-project-ref".equals(projectRef)) {
            errores.add("SUPABASE_PROJECT_REF no esta configurado o tiene un valor placeholder");
        }

        if (password == null || password.isBlank()
                || "tu-password-segura".equals(password)) {
            errores.add("SUPABASE_PASS no esta configurado o tiene un valor placeholder");
        }

        if (dbUser == null || dbUser.isBlank()
                || dbUser.contains("undefined")) {
            errores.add("SUPABASE_DB_USER no esta configurado o contiene 'undefined'");
        }

        if (dbUrl == null || dbUrl.isBlank()
                || dbUrl.contains("undefined")) {
            errores.add("SUPABASE_DB_URL no esta configurado o contiene 'undefined'");
        } else {
            if (!dbUrl.contains("sslmode=require") && !dbUrl.contains("sslmode=verify")) {
                System.err.println("[VALIDACION] ADVERTENCIA: la URL JDBC no incluye sslmode=require. "
                        + "Esto es requerido para Supabase.");
            }
            boolean usaPooler = dbUrl.contains("pooler.supabase.com");
            boolean tienePrepareThreshold = dbUrl.contains("prepareThreshold=0");
            if (usaPooler && !tienePrepareThreshold) {
                System.err.println("[VALIDACION] ADVERTENCIA: usas el pooler de Supabase pero la URL no incluye "
                        + "prepareThreshold=0. Esto puede causar errores 'prepared statement already exists'.");
            }
        }

        if (!errores.isEmpty()) {
            System.err.println();
            System.err.println("=================================================");
            System.err.println("ERROR: Configuracion de Supabase incompleta o invalida");
            System.err.println("=================================================");
            for (String error : errores) {
                System.err.println("  - " + error);
            }
            System.err.println();
            System.err.println("Configura las variables de entorno antes de arrancar en PROD:");
            System.err.println("  SUPABASE_PROJECT_REF, SUPABASE_PASS, SUPABASE_DB_USER, SUPABASE_DB_URL");
            System.err.println("=================================================");
            throw new IllegalStateException(
                    "Configuracion de Supabase invalida. Variables faltantes o con placeholders: "
                            + String.join("; ", errores));
        }

        System.out.println();
        System.out.println("=================================================");
        System.out.println("Configuracion de Supabase validada correctamente (PROD)");
        System.out.println("  Project ref: " + projectRef);
        System.out.println("  Usuario: " + dbUser);
        System.out.println("  Modo: " + (dbUrl.contains("pooler.supabase.com")
                ? "Supavisor Pooler (transaction mode)" : "Conexion directa"));
        System.out.println("=================================================");
        System.out.println();

        boolean storageEnabled = Boolean.parseBoolean(
                environment.getProperty("app.supabase.storage.enabled", "false"));
        if (storageEnabled) {
            String serviceKey = environment.getProperty("app.supabase.storage.service-role-key");
            String bucket = environment.getProperty("app.supabase.storage.bucket");
            String storageProjectRef = environment.getProperty("app.supabase.storage.project-ref");
            String storagePublicBaseUrl = environment.getProperty("app.supabase.storage.public-base-url");
            List<String> erroresStorage = new ArrayList<>();

            if (serviceKey == null || serviceKey.isBlank() || serviceKey.contains("undefined")) {
                erroresStorage.add("SUPABASE_SERVICE_ROLE_KEY no esta configurada (requerida cuando storage.enabled=true)");
            }
            if (bucket == null || bucket.isBlank()) {
                erroresStorage.add("SUPABASE_STORAGE_BUCKET no esta configurada");
            }
            if ((storageProjectRef == null || storageProjectRef.isBlank())
                    && (storagePublicBaseUrl == null || storagePublicBaseUrl.isBlank())) {
                erroresStorage.add("Debes definir SUPABASE_PROJECT_REF o SUPABASE_STORAGE_PUBLIC_BASE_URL");
            }

            if (!erroresStorage.isEmpty()) {
                System.err.println();
                System.err.println("=================================================");
                System.err.println("ERROR: Configuracion de Supabase Storage invalida");
                System.err.println("=================================================");
                for (String e : erroresStorage) {
                    System.err.println("  - " + e);
                }
                System.err.println("=================================================");
                throw new IllegalStateException(
                        "Configuracion de Supabase Storage invalida: " + String.join("; ", erroresStorage));
            }

            System.out.println("Configuracion de Supabase Storage validada (PROD)");
            System.out.println("  Bucket: " + bucket);
        } else {
            System.out.println("Supabase Storage deshabilitado (app.supabase.storage.enabled=false)");
        }

        props.put("app.supabase.validated", "true");
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }
}