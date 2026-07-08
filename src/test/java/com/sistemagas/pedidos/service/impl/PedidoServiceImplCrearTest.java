package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapperImpl;
import com.sistemagas.pedidos.mapper.PedidoMapperImpl;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
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
    private ClienteRepository clienteRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    private PedidoServiceImpl service;

    private Cliente cliente;
    private Garrafa garrafa;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .direccion("Calle 123")
                .build();

        garrafa = new Garrafa() {
            @Override public Long getId() { return 1L; }
            @Override public Integer getStockDisponible() { return 100; }
            @Override public void setStockDisponible(Integer s) { }
            @Override public BigDecimal getPrecio() { return new BigDecimal("5500.00"); }
            @Override public TipoGarrafa getTipo() { return TipoGarrafa.GARRAFA_10KG; }
        };

        GarrafaStockHelper garrafaStockHelper = new GarrafaStockHelper(garrafaRepositoryPort);
        PedidoDetalleMapperImpl pedidoDetalleMapper = new PedidoDetalleMapperImpl();
        PedidoMapperImpl pedidoMapper = new PedidoMapperImpl();

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
    @DisplayName("crear: ignora urlFotoEvidencia del request (la foto ahora se asocia al cliente)")
    void crear_urlFotoEvidenciaEnRequestSeIgnora() {
        String urlFoto = "https://example.supabase.co/storage/v1/object/public/pedidos-evidencia/pedido-1/abc.jpg";

        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .clienteId(1L)
                .direccionEntrega("Calle 123")
                .urlFotoEvidencia(urlFoto)
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.existsByUuidOffline(any())).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
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
        assertThat(captor.getValue().getCliente()).isEqualTo(cliente);
    }

    @Test
    @DisplayName("crear: persiste el pedido con cliente y detalles correctos")
    void crear_pedidoOk_persiste() {
        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .clienteId(1L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.existsByUuidOffline(any())).thenReturn(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(req);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getCliente()).isEqualTo(cliente);
        assertThat(captor.getValue().getDetalles()).hasSize(1);
    }

    @Test
    @DisplayName("crear: lanza 404 cuando el cliente no existe")
    void crear_clienteNoExiste_lanza404() {
        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .clienteId(99L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.existsByUuidOffline(any())).thenReturn(false);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

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