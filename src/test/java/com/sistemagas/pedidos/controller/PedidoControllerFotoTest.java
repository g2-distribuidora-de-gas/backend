package com.sistemagas.pedidos.controller;

import com.sistemagas.pedidos.dto.response.PedidoFotoResponse;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.security.JwtAuthenticationFilter;
import com.sistemagas.pedidos.service.PedidoService;
import com.sistemagas.pedidos.util.Constantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PedidoController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("deprecation")
class PedidoControllerFotoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/pedidos/{id}/foto: sube imagen y devuelve URL publica")
    void subirFoto_ok() throws Exception {
        String url = "https://example.supabase.co/storage/v1/object/public/pedidos-evidencia/pedido-5/abc.jpg";
        when(pedidoService.subirFoto(eq(5L), any(), any()))
                .thenReturn(PedidoFotoResponse.builder()
                        .pedidoId(5L)
                        .urlFotoEvidencia(url)
                        .nombreArchivo("fachada.jpg")
                        .contentType("image/jpeg")
                        .tamanioBytes(4L)
                        .build());

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "fachada.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes());

        mockMvc.perform(multipart(Constantes.API_PEDIDOS + "/{id}/foto", 5)
                        .file(archivo)
                        .param("descripcion", "fachada principal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.data.pedidoId").value(5))
                .andExpect(jsonPath("$.data.urlFotoEvidencia").value(url));

        verify(pedidoService).subirFoto(eq(5L), any(), eq("fachada principal"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/pedidos/{id}/foto: 404 cuando el pedido no existe")
    void subirFoto_pedidoNoExiste_404() throws Exception {
        when(pedidoService.subirFoto(eq(99L), any(), any()))
                .thenThrow(new ResourceNotFoundException(Constantes.MSG_PEDIDO_NO_ENCONTRADO));

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "fachada.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes());

        mockMvc.perform(multipart(Constantes.API_PEDIDOS + "/{id}/foto", 99)
                        .file(archivo))
                .andExpect(status().isNotFound());
    }
}