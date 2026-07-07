package com.sistemagas.pedidos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI pedidosOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API Sistema de Pedidos de Gas")
                        .description("Backend para gestion de pedidos de garrafas de gas con sincronizacion offline-first. " +
                                "Incluye endpoints para creacion y consulta de pedidos, gestion de usuarios y garrafas, " +
                                "sincronizacion offline de pedidos, actualizacion de precio de garrafas y reposicion de stock.\n\n" +
                                "**Autenticación:** Usar POST /api/auth/login para obtener un token JWT, " +
                                "luego hacer clic en 'Authorize' y pegar el token.")
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("dev@sistemagas.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT obtenido de /api/auth/login")));
    }
}