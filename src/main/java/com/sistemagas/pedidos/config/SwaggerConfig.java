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
                                "Incluye endpoints para:\n\n" +
                                "- Autenticacion y registro de usuarios (con roles PREVENTISTA, REPARTIDOR, ADMIN, SUPER_ADMIN)\n" +
                                "- Gestion de clientes (incluye foto de fachada como evidencia)\n" +
                                "- Creacion y consulta de pedidos (online y offline)\n" +
                                "- Gestion de garrafas (catalogo, precio y reposicion de stock)\n" +
                                "- Sincronizacion offline de pedidos e imagenes pendientes\n" +
                                "- Planificacion y seguimiento de rutas de reparto\n\n" +
                                "**Autenticacion:** Usar `POST /api/auth/login` para obtener un token JWT, " +
                                "luego hacer clic en 'Authorize' y pegar el token.\n\n" +
                                "**Auditoria automatica:** todas las entidades registran automaticamente el usuario " +
                                "que las creo/modifico. Campos `creadoPor` y `actualizadoPor` visibles en cada " +
                                "response. Si no hay contexto de seguridad se usa el valor `SYSTEM`.\n\n" +
                                "**Codigos de error estructurados:** todas las respuestas de error devuelven " +
                                "un envelope `ApiResponse` con `data.codigo` (string) y `data.status` (number) " +
                                "para identificar el tipo sin parsear el mensaje. Ejemplos: " +
                                "`USUARIO_NO_ENCONTRADO` (404), `NO_AUTHENTICATED` (401), " +
                                "`GARRAFA_TIPO_OBLIGATORIO` (400), `GARRAFA_CAPACIDAD_INCONSISTENTE` (400), " +
                                "`PEDIDO_DUPLICADO` (400). Ver README para lista completa.")
                        .version("0.0.1")
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