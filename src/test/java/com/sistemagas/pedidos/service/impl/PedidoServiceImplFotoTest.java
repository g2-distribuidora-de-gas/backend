package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.response.PedidoFotoResponse;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapperImpl;
import com.sistemagas.pedidos.mapper.PedidoMapperImpl;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import com.sistemagas.pedidos.support.NoopTransactionManager;
import com.sistemagas.pedidos.util.Constantes;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplFotoTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private GarrafaRepositoryPort garrafaRepositoryPort;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private PedidoServiceImpl service;

    @BeforeEach
    void setUp() {
        GarrafaStockHelper garrafaStockHelper = new GarrafaStockHelper(garrafaRepositoryPort);
        PedidoMapperImpl pedidoMapper = new PedidoMapperImpl();
        PedidoDetalleMapperImpl pedidoDetalleMapper = new PedidoDetalleMapperImpl();
        injectField(pedidoMapper, "pedidoDetalleMapper", pedidoDetalleMapper);

        TransactionTemplate txTemplate = new TransactionTemplate(new NoopTransactionManager());

        service = new PedidoServiceImpl(
                pedidoRepository,
                garrafaRepositoryPort,
                clienteRepository,
                pedidoMapper,
                pedidoDetalleMapper,
                garrafaStockHelper,
                supabaseStorageService,
                txTemplate);
    }

    @Test
    @DisplayName("subirFoto: cuando el pedido no existe lanza ResourceNotFoundException")
    void subirFoto_pedidoNoExiste_lanza404() {
        when(pedidoRepository.existsById(99L)).thenReturn(false);

        MultipartFile archivo = new MockMultipartFile(
                "archivo", "fachada.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> service.subirFoto(99L, archivo, "desc"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(Constantes.MSG_PEDIDO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("subirFoto: sube el archivo, persiste la URL en el pedido y devuelve el DTO")
    void subirFoto_ok_persisteUrl() {
        Cliente cliente = Cliente.builder().id(1L).nombre("Juan").direccion("Calle 1").build();
        Pedido pedido = Pedido.builder()
                .id(5L)
                .cliente(cliente)
                .direccionEntrega("Calle 1")
                .build();
        when(pedidoRepository.existsById(5L)).thenReturn(true);
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        String urlEsperada = "https://example.supabase.co/storage/v1/object/public/pedidos-evidencia/pedido-5/abc.jpg";
        when(supabaseStorageService.subir(eq(5L), any(MultipartFile.class), anyString()))
                .thenReturn(urlEsperada);

        MultipartFile archivo = new MockMultipartFile(
                "archivo", "fachada.jpg", "image/jpeg", "data".getBytes());

        PedidoFotoResponse resp = service.subirFoto(5L, archivo, "fachada principal");

        assertThat(resp.getPedidoId()).isEqualTo(5L);
        assertThat(resp.getUrlFotoEvidencia()).isEqualTo(urlEsperada);
        assertThat(resp.getNombreArchivo()).isEqualTo("fachada.jpg");
        assertThat(resp.getContentType()).isEqualTo("image/jpeg");
        assertThat(resp.getTamanioBytes()).isEqualTo(4L);

        assertThat(pedido.getUrlFotoEvidencia()).isEqualTo(urlEsperada);
    }

    @Test
    @DisplayName("subirFoto: si falla persistir la URL llama a eliminar como compensacion")
    void subirFoto_dbFalla_compensacion() {
        when(pedidoRepository.existsById(7L)).thenReturn(true);
        when(pedidoRepository.findById(7L)).thenThrow(new RuntimeException("DB caida"));

        String url = "https://example.supabase.co/storage/v1/object/public/pedidos-evidencia/pedido-7/abc.jpg";
        when(supabaseStorageService.subir(eq(7L), any(MultipartFile.class), any()))
                .thenReturn(url);

        MultipartFile archivo = new MockMultipartFile(
                "archivo", "fachada.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> service.subirFoto(7L, archivo, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB caida");

        verify(supabaseStorageService).eliminar(url);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo inyectar " + fieldName, e);
        }
    }
}