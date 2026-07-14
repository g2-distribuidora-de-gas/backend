package com.sistemagas.pedidos.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

public class SecurityConfigValidator implements EnvironmentPostProcessor {

    private static final String PLACEHOLDER_PREFIX = "clave-secreta-de-desarrollo";
    private static final int MIN_SECRET_LENGTH = 64;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean esProduccion = false;
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                esProduccion = true;
                break;
            }
        }
        if (!esProduccion) {
            return;
        }

        String jwtSecret = environment.getProperty("app.jwt.secret");
        List<String> errores = new ArrayList<>();

        if (jwtSecret == null || jwtSecret.isBlank()) {
            errores.add("JWT_SECRET no esta configurado (variable de entorno JWT_SECRET)");
        } else if (jwtSecret.startsWith(PLACEHOLDER_PREFIX)) {
            errores.add("JWT_SECRET tiene un valor placeholder de desarrollo. "
                    + "Genera una clave propia con al menos " + MIN_SECRET_LENGTH
                    + " caracteres aleatorios (ej: `openssl rand -base64 64`).");
        } else if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            errores.add("JWT_SECRET es demasiado corto (" + jwtSecret.length()
                    + " caracteres). Se requieren al menos " + MIN_SECRET_LENGTH + " caracteres.");
        }

        if (!errores.isEmpty()) {
            System.err.println();
            System.err.println("=================================================");
            System.err.println("ERROR: Configuracion de seguridad invalida (PROD)");
            System.err.println("=================================================");
            for (String error : errores) {
                System.err.println("  - " + error);
            }
            System.err.println();
            System.err.println("Configura la variable de entorno JWT_SECRET antes de arrancar en PROD.");
            System.err.println("=================================================");
            throw new IllegalStateException(
                    "Configuracion de seguridad invalida: " + String.join("; ", errores));
        }

        System.out.println();
        System.out.println("=================================================");
        System.out.println("Configuracion de seguridad validada correctamente (PROD)");
        System.out.println("  JWT_SECRET: " + jwtSecret.length() + " caracteres (ok)");
        System.out.println("=================================================");
        System.out.println();
    }
}