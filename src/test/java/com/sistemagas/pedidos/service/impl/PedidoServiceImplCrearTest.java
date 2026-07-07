package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapperImpl;
import com.sistemagas.pedidos.mapper.PedidoMapperImpl;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.repository.port.UsuarioRepositoryPort;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import com.sistemagas.pedidos.support.NoopTransactionManager;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplCrearTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private GarrafaRepositoryPort garrafaRepositoryPort;

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private PedidoServiceImpl service;

    private Usuario usuario;
    private Garrafa garrafa;

    @BeforeEach
    void setUp() {
        usuario = new Usuario() {
            @Override public Long getId() { return 1L; }
            @Override public String getNombre() { return "Juan"; }
            @Override public String getApellido() { return "Perez"; }
        };

        garrafa = new Garrafa() {
            @Override public Long getId() { return 1L; }
            @Override public Integer getStockDisponible() { return 100; }
            @Override public void setStockDisponible(Integer s) { }
            @Override public BigDecimal getPrecio() { return new BigDecimal("5500.00"); }
            @Override public TipoGarrafa getTipo() { return TipoGarrafa.GARRAFA_10KG; }
        };

        GarrafaStockHelper garrafaStockHelper = new GarrafaStockHelper(garrafaRepositoryPort);
        PedidoMapperImpl pedidoMapper = new PedidoMapperImpl();
        PedidoDetalleMapperImpl pedidoDetalleMapper = new PedidoDetalleMapperImpl();
        injectField(pedidoMapper, "pedidoDetalleMapper", pedidoDetalleMapper);

        TransactionTemplate txTemplate = new TransactionTemplate(new NoopTransactionManager());

        service = new PedidoServiceImpl(
                pedidoRepository,
                garrafaRepositoryPort,
                usuarioRepositoryPort,
                pedidoMapper,
                pedidoDetalleMapper,
                garrafaStockHelper,
                supabaseStorageService,
                txTemplate);
    }

    @Test
    @DisplayName("crear: persiste urlFotoEvidencia cuando viene en el request")
    void crear_persisteUrlFotoEvidencia() {
        String urlFoto = "https://example.supabase.co/storage/v1/object/public/pedidos-evidencia/pedido-1/abc.jpg";

        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .usuarioId(1L)
                .direccionEntrega("Calle 123")
                .urlFotoEvidencia(urlFoto)
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.existsByUuidOffline(any())).thenReturn(false);
        when(usuarioRepositoryPort.findById(1L)).thenReturn(Optional.of(usuario));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        service.crear(req);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getUrlFotoEvidencia()).isEqualTo(urlFoto);
    }

    @Test
    @DisplayName("crear: tolera urlFotoEvidencia null (no rompe)")
    void crear_urlFotoEvidenciaNull_noRompe() {
        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .usuarioId(1L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.existsByUuidOffline(any())).thenReturn(false);
        when(usuarioRepositoryPort.findById(1L)).thenReturn(Optional.of(usuario));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(req);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getUrlFotoEvidencia()).isNull();
    }

    @Test
    @DisplayName("crear: lanza 404 cuando el usuario no existe")
    void crear_usuarioNoExiste_lanza404() {
        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .usuarioId(99L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.existsByUuidOffline(any())).thenReturn(false);
        when(usuarioRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(ResourceNotFoundException.class);
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