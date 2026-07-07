package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.SupabaseStorageProperties;
import com.sistemagas.pedidos.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
    @DisplayName("subir: con respuesta 2xx del WebClient devuelve la URL publica del bucket")
    void subir_ok_retornaUrlPublica() throws IOException {
        stubWebClientOk();

        String url = service.subir(42L, jpeg(), "Fachada principal");
        assertThat(url)
                .startsWith("https://test-project-ref.supabase.co/storage/v1/object/public/pedidos-evidencia/pedido-42/")
                .endsWith(".jpg");
        assertThat(url).contains("pedido-42");
    }

    @Test
    @DisplayName("subir: con respuesta 4xx lanza BusinessException con status code")
    void subir_respuestaError_lanzaExcepcion() throws IOException {
        MockHttpOutputMessage out = new MockHttpOutputMessage();
        out.getBody().write("bucket not found".getBytes(StandardCharsets.UTF_8));
        ClientHttpResponse httpResponse = new StubClientHttpResponse(
                org.springframework.http.HttpStatus.NOT_FOUND, out);

        WebClientResponseException wcre = WebClientResponseException.create(
                404, "Not Found", org.springframework.http.HttpHeaders.EMPTY, null, null);
        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        lenient().when(responseSpec.toBodilessEntity())
                .thenThrow(wcre);

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

    private static class StubClientHttpResponse implements ClientHttpResponse {
        private final org.springframework.http.HttpStatusCode status;
        private final MockHttpOutputMessage body;

        StubClientHttpResponse(org.springframework.http.HttpStatusCode status, MockHttpOutputMessage body) {
            this.status = status;
            this.body = body;
        }

        @Override public HttpStatusCode getStatusCode() { return status; }
        @Override public int getRawStatusCode() { return status.value(); }
        @Override public String getStatusText() { return status.toString(); }
        @Override public org.springframework.http.HttpHeaders getHeaders() { return new org.springframework.http.HttpHeaders(); }
        @Override public org.springframework.core.io.buffer.DataBufferFactory bufferFactory() { return null; }
        @Override public Flux<org.springframework.core.io.buffer.DataBuffer> getBody() { return Flux.empty(); }
        @Override public Mono<Void> writeTo(java.util.function.Supplier<? extends org.springframework.core.io.buffer.DataBuffer> body, org.springframework.core.io.buffer.DataBufferFactory bufferFactory) { return Mono.empty(); }
        @Override public Mono<Void> writeAndFlushWith(java.util.function.Supplier<? extends org.springframework.core.io.buffer.Publisher<? extends org.springframework.core.io.buffer.DataBuffer>> body) { return Mono.empty(); }
        @Override public org.springframework.http.client.reactive.ClientHttpResponse logPrefix() { return this; }
        @Override public URI getURI() { return URI.create("https://test.supabase.co"); }
        @Override public boolean isCommitted() { return true; }
        @Override public Mono<Void> setComplete() { return Mono.empty(); }
        @Override public ByteArrayOutputStream getBodyAsBytes() { return null; }
        @Override public <T> T getBodyToMono(Class<T> elementClass) { return null; }
        @Override public <T> Flux<T> getBodyToFlux(Class<T> elementClass) { return Flux.empty(); }
        @Override public Mono<Void> releaseBody() { return Mono.empty(); }
        @Override public Mono<Void> writeAndFlushWith(java.util.function.Supplier<? extends org.reactivestreams.Publisher<? extends org.springframework.core.io.buffer.DataBuffer>> body) { return Mono.empty(); }
    }
}