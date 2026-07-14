package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapper;
import com.sistemagas.pedidos.mapper.PedidoDetalleMapperImpl;
import com.sistemagas.pedidos.mapper.PedidoMapper;
import com.sistemagas.pedidos.mapper.PedidoMapperImpl;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.repository.ClienteRepository;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.service.ClienteFotoService;
import com.sistemagas.pedidos.util.Constantes;
import com.sistemagas.pedidos.util.GarrafaStockHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SincronizacionServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private GarrafaRepositoryPort garrafaRepositoryPort;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteFotoService clienteFotoService;

    @Mock
    private UsuarioRepository usuarioRepository;

    private PedidoMapper pedidoMapper;
    private PedidoDetalleMapper pedidoDetalleMapper;
    private GarrafaStockHelper garrafaStockHelper;

    private SincronizacionServiceImpl service;

    private Cliente cliente;
    private Garrafa garrafa10;
    private Garrafa garrafa15;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .build();

        garrafa10 = makeGarrafa(1L, TipoGarrafa.GARRAFA_10KG, new BigDecimal("5500.00"), 100);
        garrafa15 = makeGarrafa(2L, TipoGarrafa.GARRAFA_15KG, new BigDecimal("7800.00"), 50);

        pedidoDetalleMapper = new PedidoDetalleMapperImpl();
        pedidoMapper = new PedidoMapperImpl();

        garrafaStockHelper = new GarrafaStockHelper(garrafaRepositoryPort);

        SincronizacionPedidoSaver pedidoSaver = new SincronizacionPedidoSaver(
                pedidoRepository, garrafaRepositoryPort, pedidoMapper, garrafaStockHelper, usuarioRepository);
        SincronizacionPedidoProcessor pedidoProcessor = new SincronizacionPedidoProcessor(
                clienteRepository, garrafaStockHelper, pedidoSaver, clienteFotoService);

        service = new SincronizacionServiceImpl(pedidoRepository, pedidoProcessor);
    }

    @Test
    @DisplayName("Sync: pedido nuevo con 2 detalles distintos se guarda y descuenta stock por cantidad")
    void sync_pedidoNuevo_conMultiplesDetalles() {
        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-1")
                .clienteId(1L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(
                        PedidoDetalleRequest.builder().garrafaId(1L).cantidad(2).build(),
                        PedidoDetalleRequest.builder().garrafaId(2L).cantidad(1).build()
                ))
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of());
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa10));
        when(garrafaRepositoryPort.findByIdForUpdate(2L)).thenReturn(Optional.of(garrafa15));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder().pedidos(List.of(req)).build(), "test@test.com");

        assertThat(response.getProcesados()).hasSize(1);
        assertThat(response.getProcesados().get(0).getUuidOffline()).isEqualTo("uuid-1");
        assertThat(response.getProcesados().get(0).getPedidoId()).isEqualTo(42L);
        assertThat(response.getDuplicados()).isEmpty();
        assertThat(response.getErrores()).isEmpty();

        assertThat(garrafa10.getStockDisponible()).isEqualTo(98);
        assertThat(garrafa15.getStockDisponible()).isEqualTo(49);

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedidoCaptor.capture());
        Pedido pedidoGuardado = pedidoCaptor.getValue();
        assertThat(pedidoGuardado.getDetalles()).hasSize(2);
        assertThat(pedidoGuardado.getDetalles().get(0).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("11000.00"));
        assertThat(pedidoGuardado.getDetalles().get(1).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("7800.00"));
    }

    @Test
    @DisplayName("Sync: pedido con uuidOffline existente se marca como duplicado sin descontar stock")
    void sync_pedidoDuplicado() {
        Pedido existente = Pedido.builder()
                .id(10L)
                .uuidOffline("uuid-dup")
                .cliente(cliente)
                .direccionEntrega("Calle 123")
                .build();

        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-dup")
                .clienteId(1L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(
                        PedidoDetalleRequest.builder().garrafaId(1L).cantidad(2).build()
                ))
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of(existente));

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder().pedidos(List.of(req)).build(), "test@test.com");

        assertThat(response.getDuplicados()).containsExactly("uuid-dup");
        assertThat(response.getProcesados()).isEmpty();
        assertThat(response.getErrores()).isEmpty();
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verify(garrafaRepositoryPort, never()).save(any(Garrafa.class));
    }

    @Test
    @DisplayName("Sync: lote mixto (nuevo + duplicado + sin uuid + sin detalles) se procesa correctamente")
    void sync_loteMixto() {
        Pedido existente = Pedido.builder()
                .id(10L)
                .uuidOffline("uuid-dup")
                .cliente(cliente)
                .direccionEntrega("Calle 123")
                .build();

        PedidoRequest nuevo = PedidoRequest.builder()
                .uuidOffline("uuid-new")
                .clienteId(1L)
                .direccionEntrega("Calle nueva")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        PedidoRequest duplicado = PedidoRequest.builder()
                .uuidOffline("uuid-dup")
                .clienteId(1L)
                .direccionEntrega("Calle 123")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        PedidoRequest sinUuid = PedidoRequest.builder()
                .clienteId(1L)
                .direccionEntrega("Calle sin uuid")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        PedidoRequest sinDetalles = PedidoRequest.builder()
                .uuidOffline("uuid-empty")
                .clienteId(1L)
                .direccionEntrega("Calle sin detalles")
                .detalles(List.of())
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of(existente));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa10));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder()
                        .pedidos(List.of(nuevo, duplicado, sinUuid, sinDetalles))
                        .build(), "test@test.com");

        assertThat(response.getProcesados()).hasSize(1);
        assertThat(response.getDuplicados()).containsExactly("uuid-dup");
        assertThat(response.getErrores()).hasSize(2);
        assertThat(response.getErrores().get(0).getMotivo()).contains("uuidOffline es obligatorio");
        assertThat(response.getErrores().get(1).getMotivo()).contains("al menos un detalle");
        assertThat(response.getTotal()).isEqualTo(4);
    }

    @Test
    @DisplayName("Sync: error inesperado al guardar un pedido no rompe el lote, se reporta y continua")
    void sync_errorInesperado_noRompeLote() {
        PedidoRequest req1 = PedidoRequest.builder()
                .uuidOffline("uuid-falla")
                .clienteId(1L)
                .direccionEntrega("Calle 1")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        PedidoRequest req2 = PedidoRequest.builder()
                .uuidOffline("uuid-ok")
                .clienteId(1L)
                .direccionEntrega("Calle 2")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of());
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa10));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));

        when(pedidoRepository.save(any(Pedido.class)))
                .thenThrow(new RuntimeException("DB no disponible"))
                .thenAnswer(inv -> {
                    Pedido p = inv.getArgument(0);
                    p.setId(7L);
                    return p;
                });

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder().pedidos(List.of(req1, req2)).build(), "test@test.com");

        assertThat(response.getErrores()).hasSize(1);
        assertThat(response.getErrores().get(0).getUuidOffline()).isEqualTo("uuid-falla");
        assertThat(response.getProcesados()).hasSize(1);
        assertThat(response.getProcesados().get(0).getUuidOffline()).isEqualTo("uuid-ok");
    }

    @Test
    @DisplayName("GarrafaStockHelper: garrafa no encontrada lanza ResourceNotFoundException")
    void helper_garrafaNoExiste_lanza404() {
        when(garrafaRepositoryPort.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        garrafaStockHelper.cargarYValidar(List.of(
                                PedidoDetalleRequest.builder().garrafaId(99L).cantidad(1).build())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(Constantes.MSG_GARRAFA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("GarrafaStockHelper: stock insuficiente lanza BusinessException")
    void helper_stockInsuficiente_lanza400() {
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa10));
        garrafa10.setStockDisponible(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        garrafaStockHelper.cargarYValidar(List.of(
                                PedidoDetalleRequest.builder().garrafaId(1L).cantidad(5).build())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    private Garrafa makeGarrafa(Long id, TipoGarrafa tipo, BigDecimal precio, Integer stock) {
        return new Garrafa() {
            private Integer stockRef = stock;
            @Override public Long getId() { return id; }
            @Override public Integer getStockDisponible() { return stockRef; }
            @Override public void setStockDisponible(Integer s) { this.stockRef = s; }
            @Override public BigDecimal getPrecio() { return precio; }
            @Override public TipoGarrafa getTipo() { return tipo; }
        };
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