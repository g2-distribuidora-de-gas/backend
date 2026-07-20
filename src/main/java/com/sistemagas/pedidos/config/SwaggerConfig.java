package com.sistemagas.pedidos.config;

import com.sistemagas.pedidos.dto.realtime.ErrorWsDto;
import com.sistemagas.pedidos.dto.realtime.EventoRutaWsDto;
import com.sistemagas.pedidos.dto.realtime.PosicionBroadcastDto;
import com.sistemagas.pedidos.dto.realtime.PosicionRepartidorDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String TAG_REALTIME = "Realtime";
    private static final String DOC_WS_URL = "https://github.com/Equipo2Garrafa/backend/blob/feature/websockets/docs/WEBSOCKETS_FRONTEND.md";

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
                                "- Sincronizacion offline de pedidos, clientes, paradas y cambios de estado de rutas\n" +
                                "- Planificacion, seguimiento y reporte de rutas de reparto\n" +
                                "- **Tracking GPS en tiempo real de repartidores via STOMP/WebSocket " +
                                "(los DTOs aparecen en la seccion `Schemas` debajo; ver " +
                                "`docs/WEBSOCKETS_FRONTEND.md` para la guia de integracion completa)**\n\n" +
                                "**Autenticacion:** Usar `POST /api/auth/login` para obtener un token JWT, " +
                                "luego hacer clic en 'Authorize' y pegar el token. El mismo token sirve para " +
                                "los endpoints REST y para abrir la conexion STOMP (`ws(s)://host/ws?token=<jwt>`).\n\n" +
                                "**Auditoria automatica:** todas las entidades registran automaticamente el usuario " +
                                "que las creo/modifico. Campos `creadoPor` y `actualizadoPor` visibles en cada " +
                                "response. Si no hay contexto de seguridad se usa el valor `SYSTEM`.\n\n" +
                                "**Codigos de error estructurados:** todas las respuestas de error devuelven " +
                                "un envelope `ApiResponse` con `data.codigo` (string) y `data.status` (number) " +
                                "para identificar el tipo sin parsear el mensaje. Ejemplos: " +
                                "`USUARIO_NO_ENCONTRADO` (404), `NO_AUTHENTICATED` (401), " +
                                "`GARRAFA_TIPO_OBLIGATORIO` (400), `GARRAFA_CAPACIDAD_INCONSISTENTE` (400), " +
                                "`PEDIDO_DUPLICADO` (400), `TRANSICION_ESTADO_RUTA_INVALIDA` (400). " +
                                "Ver README para lista completa.\n\n" +
                                "**Sincronizacion offline:** ver seccion `Sincronizacion` en Swagger UI para " +
                                "los endpoints batch (`/api/sincronizar`, `/api/sincronizar/clientes`, " +
                                "`/api/sincronizar/paradas`, `/api/sincronizar/rutas`).\n\n" +
                                "**WebSocket / STOMP (tag `Realtime`):** los endpoints STOMP no se exponen como " +
                                "REST, pero los DTOs JSON que viajan por los topicos estan documentados en la " +
                                "seccion `Schemas` (`PosicionRepartidorDto`, `PosicionBroadcastDto`, " +
                                "`EventoRutaWsDto`, `ErrorWsDto`). Endpoints: `ws://host/ws` (SockJS) y " +
                                "`ws://host/ws-native` (nativo). Auth via `?token=<jwt>` o " +
                                "`Authorization: Bearer ...`. Topicos: `/app/rutas/{id}/posicion` (envia), " +
                                "`/topic/rutas/{id}/posiciones` (broadcast), `/topic/rutas/{id}/eventos`, " +
                                "`/user/queue/errors`. Guia completa: " + DOC_WS_URL + ".")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("dev@sistemagas.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Guia de integracion WebSocket / STOMP para el frontend")
                        .url(DOC_WS_URL))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Dev local"),
                        new Server().url("https://api.sistemagas.com").description("Produccion")))
                .tags(List.of(
                        new Tag().name(TAG_REALTIME)
                                .description("Tracking GPS en tiempo real via STOMP/WebSocket. " +
                                        "Los DTOs JSON que viajan por los topicos estan en `Schemas` " +
                                        "(PosicionRepartidorDto, PosicionBroadcastDto, EventoRutaWsDto, ErrorWsDto). " +
                                        "Ver guia: " + DOC_WS_URL)))
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

    /**
     * Springdoc-openapi solo incluye en la seccion `Schemas` los tipos referenciados por al menos
     * un endpoint REST. Los DTOs de WebSocket se usan unicamente en `@MessageMapping` (no REST),
     * asi que los registramos via {@link OpenApiCustomizer} para que aparezcan en Swagger UI.
     */
    @Bean
    public OpenApiCustomizer realtimeSchemasCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            Components components = openApi.getComponents();
            ModelConverters converter = ModelConverters.getInstance();
            registerSchema(components, converter, "PosicionRepartidorDto", PosicionRepartidorDto.class);
            registerSchema(components, converter, "PosicionBroadcastDto", PosicionBroadcastDto.class);
            registerSchema(components, converter, "EventoRutaWsDto", EventoRutaWsDto.class);
            registerSchema(components, converter, "ErrorWsDto", ErrorWsDto.class);
        };
    }

    private static void registerSchema(Components components,
                                       ModelConverters converter,
                                       String name,
                                       Class<?> clazz) {
        var schemas = converter.readAll(clazz);
        if (schemas != null && !schemas.isEmpty()) {
            components.addSchemas(name, schemas.values().iterator().next());
        }
    }
}
