package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.SupabaseStorageProperties;
import com.sistemagas.pedidos.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private SupabaseStorageProperties properties;
    private SupabaseStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new SupabaseStorageProperties();
        properties.setEnabled(true);
        properties.setProjectRef("test-project-ref");
        properties.setServiceRoleKey("test-service-role-key");
        properties.setBucket("pedidos-evidencia");
        properties.setMaxFileSizeBytes(5L * 1024L * 1024L);
        properties.setAllowedContentTypes(List.of("image/jpeg", "image/png", "image/webp"));

        service = new SupabaseStorageServiceImpl(webClient, properties);
    }

    @Test
    @DisplayName("subir: lanza BusinessException si Storage esta deshabilitado")
    void subir_storageDeshabilitado_lanzaExcepcion() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> service.subir(1L, jpeg(), "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deshabilitado");
    }

    @Test
    @DisplayName("subir: lanza BusinessException si archivo es null o vacio")
    void subir_archivoVacio_lanzaExcepcion() {
        MultipartFile vacio = new MockMultipartFile("archivo", new byte[0]);
        assertThatThrownBy(() -> service.subir(1L, vacio, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("subir: lanza BusinessException si el content-type no esta permitido")
    void subir_contentTypeNoPermitido_lanzaExcepcion() {
        MultipartFile pdf = new MockMultipartFile(
                "archivo", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes());
        assertThatThrownBy(() -> service.subir(1L, pdf, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tipo de archivo no permitido");
    }

    @Test
    @DisplayName("subir: lanza BusinessException si excede el tamano maximo")
    void subir_excedeTamano_lanzaExcepcion() {
        byte[] grande = new byte[(int) (properties.getMaxFileSizeBytes() + 1)];
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "fachada.jpg", MediaType.IMAGE_JPEG_VALUE, grande);

        assertThatThrownBy(() -> service.subir(1L, archivo, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tamano maximo");
    }

    @Test
    @DisplayName("subir: con respuesta 2xx del WebClient devuelve el object path (no la URL publica)")
    void subir_ok_retornaObjectPath() {
        stubWebClientOk();

        String objectPath = service.subir(42L, jpeg(), "Fachada principal");
        assertThat(objectPath)
                .startsWith("pedido-42/")
                .endsWith(".jpg");
    }

    @Test
    @DisplayName("subir: con error HTTP del WebClient lanza BusinessException")
    void subir_respuestaError_lanzaExcepcion() {
        WebClientResponseException wcre = WebClientResponseException.create(
                404, "Not Found", org.springframework.http.HttpHeaders.EMPTY, null, null);

        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.toBodilessEntity()).thenThrow(wcre);

        assertThatThrownBy(() -> service.subir(7L, jpeg(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Supabase Storage");
    }

    private MultipartFile jpeg() {
        return new MockMultipartFile(
                "archivo", "fachada.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-jpeg-bytes".getBytes());
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientOk() {
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.just(org.springframework.http.ResponseEntity.ok().build()));
    }
}