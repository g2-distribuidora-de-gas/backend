package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.ClienteFotoPendiente;
import com.sistemagas.pedidos.repository.ClienteFotoPendienteRepository;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// (String) any() cast used in this test to disambiguate SupabaseStorageService.subir overloads

@ExtendWith(MockitoExtension.class)
class ClienteFotoServiceImplTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private ClienteFotoPendienteRepository clienteFotoPendienteRepository;
    @Mock private SupabaseStorageService supabaseStorageService;

    @InjectMocks private ClienteFotoServiceImpl service;

    private Cliente cliente;
    private MultipartFile archivo;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(7L)
                .nombre("Juan")
                .fotoEvidenciaPath(null)
                .build();

        archivo = new org.springframework.mock.web.MockMultipartFile(
                "archivo", "fachada.jpg", "image/jpeg", "fake-bytes".getBytes());
    }

    @Test
    @DisplayName("subirImagenPendiente: cliente ya tiene pendiente retorna path existente (idempotencia)")
    void subirImagenPendiente_pendienteExistente_retornaPathExistente() {
        ClienteFotoPendiente existente = ClienteFotoPendiente.builder()
                .id(1L)
                .clienteId(7L)
                .objectPath("cliente-pending/7/fachada.jpg")
                .uploadedAt(Instant.now())
                .build();
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteFotoPendienteRepository.findFirstByClienteIdOrderByUploadedAtDescIdDesc(7L))
                .thenReturn(Optional.of(existente));

        String path = service.subirImagenPendiente(7L, archivo, "fachada nueva");

        assertThat(path).isEqualTo("cliente-pending/7/fachada.jpg");
        verify(supabaseStorageService, never()).subir((String) any(), any(), any());
        verify(clienteFotoPendienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("subirImagenPendiente: cliente ya tiene foto asociada tira 409")
    void subirImagenPendiente_clienteYaTieneFoto_lanza409() {
        cliente.setFotoEvidenciaPath("cliente-7/fachada.jpg");
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteFotoPendienteRepository.findFirstByClienteIdOrderByUploadedAtDescIdDesc(7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subirImagenPendiente(7L, archivo, "fachada"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("El cliente ya tiene foto de evidencia asociada");

        verify(supabaseStorageService, never()).subir((String) any(), any(), any());
    }

    @Test
    @DisplayName("subirImagenPendiente: carrera DB UNIQUE -> recupera path ganador en vez de tirar")
    void subirImagenPendiente_carreraUnicaConstraint_recuperaPathGanador() {
        ClienteFotoPendiente ganador = ClienteFotoPendiente.builder()
                .id(99L)
                .clienteId(7L)
                .objectPath("cliente-pending/7/fachada-ganadora.jpg")
                .uploadedAt(Instant.now())
                .build();

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteFotoPendienteRepository.findFirstByClienteIdOrderByUploadedAtDescIdDesc(7L))
                .thenReturn(Optional.empty())                                  // 1ra busqueda: vacia
                .thenReturn(Optional.of(ganador));                             // 2da busqueda tras excepcion
        when(supabaseStorageService.subir((String) any(), any(), any()))
                .thenReturn("cliente-pending/7/fachada-perdedora.jpg");
        when(clienteFotoPendienteRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uk_cliente_foto_pendiente"));

        String path = service.subirImagenPendiente(7L, archivo, "fachada");

        assertThat(path).isEqualTo("cliente-pending/7/fachada-ganadora.jpg");
        verify(supabaseStorageService, times(1)).subir((String) any(), any(), any());
    }

    @Test
    @DisplayName("subirImagenPendiente: cliente no existe lanza 404")
    void subirImagenPendiente_clienteNoExiste_lanza404() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subirImagenPendiente(999L, archivo, "fachada"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado");
    }
}
