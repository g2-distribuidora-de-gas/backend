package com.sistemagas.pedidos.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemagas.pedidos.dto.realtime.PosicionBroadcastDto;
import com.sistemagas.pedidos.dto.realtime.PosicionRepartidorDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.profiles.active=test",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.datasource.url=jdbc:h2:mem:ws-swagger-testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "app.jwt.secret=clave-secreta-de-desarrollo-cambiar-en-produccion-min-64-chars-!!!!",
                "app.jwt.expiration-ms=86400000",
                "app.supabase.storage.enabled=false",
                "locationiq.api-key=test"
        }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SwaggerConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /v3/api-docs expone tag 'Realtime' para WebSocket")
    void openApiTieneTagRealtime() throws Exception {
        JsonNode root = fetchApiDocs();

        boolean hasRealtimeTag = false;
        for (JsonNode tag : root.path("tags")) {
            if ("Realtime".equals(tag.path("name").asText())) {
                hasRealtimeTag = true;
                break;
            }
        }
        assertThat(hasRealtimeTag)
                .as("El tag Realtime debe estar en la lista de tags del OpenAPI")
                .isTrue();
    }

    @Test
    @DisplayName("GET /v3/api-docs expone los 4 DTOs WebSocket en components.schemas")
    void openApiRegistraSchemasWebSocket() throws Exception {
        JsonNode schemas = fetchApiDocs().path("components").path("schemas");

        assertThat(schemas.has("PosicionRepartidorDto"))
                .as("PosicionRepartidorDto debe estar registrado").isTrue();
        assertThat(schemas.has("PosicionBroadcastDto"))
                .as("PosicionBroadcastDto debe estar registrado").isTrue();
        assertThat(schemas.has("EventoRutaWsDto"))
                .as("EventoRutaWsDto debe estar registrado").isTrue();
        assertThat(schemas.has("ErrorWsDto"))
                .as("ErrorWsDto debe estar registrado").isTrue();
    }

    @Test
    @DisplayName("La descripcion del API menciona el flujo WebSocket y STOMP")
    void openApiMencionaWebSocketEnInfo() throws Exception {
        JsonNode root = fetchApiDocs();
        String desc = root.path("info").path("description").asText();
        assertThat(desc).contains("WebSocket").contains("STOMP");
    }

    @Test
    @DisplayName("Los DTOs WebSocket serializan correctamente via Jackson")
    void schemasSerializanCorrectamente() throws Exception {
        ObjectMapper om = objectMapper.copy();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PosicionRepartidorDto a = PosicionRepartidorDto.builder()
                .latitud(new BigDecimal("-26.2"))
                .longitud(new BigDecimal("-58.2"))
                .origen("GPS")
                .timestampCliente(Instant.now())
                .build();
        String jsonA = om.writeValueAsString(a);
        assertThat(jsonA).contains("\"latitud\":-26.2").contains("\"origen\":\"GPS\"");

        PosicionBroadcastDto b = PosicionBroadcastDto.builder()
                .rutaId(10L).repartidorId(5L).repartidorNombre("Juan")
                .estadoRuta(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                .latitud(new BigDecimal("-26.2"))
                .longitud(new BigDecimal("-58.2"))
                .serverTimestamp(Instant.now())
                .build();
        String jsonB = om.writeValueAsString(b);
        assertThat(jsonB).contains("\"repartidorNombre\":\"Juan\"")
                .contains("\"estadoRuta\":\"EN_CURSO\"");

        com.sistemagas.pedidos.dto.realtime.EventoRutaWsDto e =
                com.sistemagas.pedidos.dto.realtime.EventoRutaWsDto.builder()
                        .tipo("CAMBIO_ESTADO_RUTA").rutaId(10L)
                        .estadoAnterior(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                        .estadoNuevo(com.sistemagas.pedidos.enums.EstadoRuta.COMPLETADA)
                        .timestamp(Instant.now())
                        .build();
        String jsonE = om.writeValueAsString(e);
        assertThat(jsonE).contains("\"tipo\":\"CAMBIO_ESTADO_RUTA\"")
                .contains("\"estadoNuevo\":\"COMPLETADA\"");

        com.sistemagas.pedidos.dto.realtime.ErrorWsDto err =
                com.sistemagas.pedidos.dto.realtime.ErrorWsDto.builder()
                        .codigo("RUTA_NO_TRANSMITE").mensaje("La ruta 10 esta COMPLETADA")
                        .timestamp(Instant.now())
                        .build();
        String jsonErr = om.writeValueAsString(err);
        assertThat(jsonErr).contains("\"codigo\":\"RUTA_NO_TRANSMITE\"");
    }

    private JsonNode fetchApiDocs() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
