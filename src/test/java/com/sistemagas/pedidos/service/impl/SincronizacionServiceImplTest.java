package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.PedidoDetalleRequest;
import com.sistemagas.pedidos.dto.request.PedidoRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionClienteRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionParadaItemRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionParadasRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRutaItemRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRutasRequest;
import com.sistemagas.pedidos.dto.request.SincronizacionRequest;
import com.sistemagas.pedidos.dto.response.SincronizacionClienteResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionParadasResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionRutasResponse;
import com.sistemagas.pedidos.dto.response.SincronizacionResponse;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoRuta;
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
import com.sistemagas.pedidos.service.impl.SincronizacionParadaProcessor;
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

import org.springframework.http.HttpStatus;

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

    @Mock
    private com.sistemagas.pedidos.service.ClienteService clienteService;

    @Mock
    private SincronizacionParadaProcessor paradaProcessor;

    @Mock
    private SincronizacionRutaProcessor rutaProcessor;



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

        SincronizacionClienteProcessor clienteProcessor = new SincronizacionClienteProcessor(clienteService);

        service = new SincronizacionServiceImpl(pedidoRepository, clienteRepository, pedidoProcessor, clienteProcessor, paradaProcessor, rutaProcessor);
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
        assertThat(pedidoGuardado.getDetalles().get(0).getCantidad()).isEqualTo(2);
        assertThat(pedidoGuardado.getDetalles().get(0).getPrecioUnitario())
                .isEqualByComparingTo(new BigDecimal("5500.00"));
        assertThat(pedidoGuardado.getDetalles().get(1).getCantidad()).isEqualTo(1);
        assertThat(pedidoGuardado.getDetalles().get(1).getPrecioUnitario())
                .isEqualByComparingTo(new BigDecimal("7800.00"));
    }

    @Test
    @DisplayName("Sync: pedido con clienteUuidOffline se resuelve por UUID y NO consulta por id")
    void sync_pedidoConClienteUuidOffline_seResuelveCorrectamente() {
        Cliente clienteOffline = Cliente.builder()
                .id(99L)
                .uuidOffline("uuid-cli-offline")
                .nombre("Cliente offline")
                .build();

        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-ped-1")
                .clienteUuidOffline("uuid-cli-offline")
                .direccionEntrega("Calle offline 123")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of());
        when(clienteRepository.findByUuidOffline("uuid-cli-offline")).thenReturn(Optional.of(clienteOffline));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa10));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(123L);
            return p;
        });

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder().pedidos(List.of(req)).build(), "test@test.com");

        assertThat(response.getProcesados()).hasSize(1);
        assertThat(response.getProcesados().get(0).getUuidOffline()).isEqualTo("uuid-ped-1");
        assertThat(response.getProcesados().get(0).getPedidoId()).isEqualTo(123L);
        assertThat(response.getErrores()).isEmpty();

        verify(clienteRepository).findByUuidOffline("uuid-cli-offline");
        verify(clienteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Sync: clienteUuidOffline inexistente se reporta como error y NO se descuenta stock")
    void sync_pedidoConClienteUuidOfflineNoExiste_seReportaComoError() {
        PedidoRequest req = PedidoRequest.builder()
                .uuidOffline("uuid-ped-404")
                .clienteUuidOffline("uuid-inexistente")
                .direccionEntrega("Calle sin cliente")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of());
        when(clienteRepository.findByUuidOffline("uuid-inexistente")).thenReturn(Optional.empty());

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder().pedidos(List.of(req)).build(), "test@test.com");

        assertThat(response.getProcesados()).isEmpty();
        assertThat(response.getErrores()).hasSize(1);
        assertThat(response.getErrores().get(0).getUuidOffline()).isEqualTo("uuid-ped-404");
        assertThat(response.getErrores().get(0).getMotivo()).contains("uuidOffline: uuid-inexistente");

        verify(clienteRepository).findByUuidOffline("uuid-inexistente");
        verify(clienteRepository, never()).findById(any());
        verify(garrafaRepositoryPort, never()).findByIdForUpdate(any());
        verify(pedidoRepository, never()).save(any(Pedido.class));
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
    @DisplayName("Sync pedidos: mismo uuidOffline dos veces en el batch se reporta como duplicado sin DataIntegrityViolation")
    void sync_pedidoDuplicadoIntraBatch_seReportaComoDuplicadoSinProcesar() {
        PedidoRequest reqA = PedidoRequest.builder()
                .uuidOffline("uuid-dup-intra")
                .clienteId(1L)
                .direccionEntrega("Calle A")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();
        PedidoRequest reqB = PedidoRequest.builder()
                .uuidOffline("uuid-dup-intra")
                .clienteId(1L)
                .direccionEntrega("Calle A")
                .detalles(List.of(PedidoDetalleRequest.builder().garrafaId(1L).cantidad(1).build()))
                .build();

        when(pedidoRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of());
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(garrafaRepositoryPort.findByIdForUpdate(1L)).thenReturn(Optional.of(garrafa10));
        when(garrafaRepositoryPort.save(any(Garrafa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(500L);
            return p;
        });

        SincronizacionResponse response = service.procesarPedidosOffline(
                SincronizacionRequest.builder().pedidos(List.of(reqA, reqB)).build(),
                "test@test.com");

        assertThat(response.getProcesados()).hasSize(1);
        assertThat(response.getDuplicados()).containsExactly("uuid-dup-intra");
        assertThat(response.getErrores()).isEmpty();
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Sync clientes: mismo uuidOffline dos veces en el batch se reporta como duplicado sin DataIntegrityViolation")
    void sync_clienteDuplicadoIntraBatch_seReportaComoDuplicadoSinProcesar() {
        com.sistemagas.pedidos.dto.request.ClienteRequest cliA =
                new com.sistemagas.pedidos.dto.request.ClienteRequest();
        cliA.setUuidOffline("uuid-cli-intra");
        cliA.setNombre("A");
        cliA.setDireccion("Calle 1");

        com.sistemagas.pedidos.dto.request.ClienteRequest cliB =
                new com.sistemagas.pedidos.dto.request.ClienteRequest();
        cliB.setUuidOffline("uuid-cli-intra");
        cliB.setNombre("B");
        cliB.setDireccion("Calle 2");

        when(clienteRepository.findByUuidOfflineIn(anyList())).thenReturn(List.of());
        when(clienteService.crearCliente(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(700L);
            return c;
        });

        SincronizacionClienteResponse response = service.procesarClientesOffline(
                SincronizacionClienteRequest.builder().clientes(List.of(cliA, cliB)).build());

        assertThat(response.getDuplicados()).containsExactly("uuid-cli-intra");
        assertThat(response.getProcesados()).hasSize(1);
        assertThat(response.getProcesados().get(0).getUuidOffline()).isEqualTo("uuid-cli-intra");
        assertThat(response.getErrores()).isEmpty();
        verify(clienteService, times(1)).crearCliente(any(Cliente.class));
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

    @Test
    @DisplayName("Paradas: mismo uuidOffline en batch → segunda va a procesados sin rebotar")
    void paradas_mismoUuidEnBatch_segundaVaaProcesadosSinRebotar() {
        SincronizacionParadaItemRequest a = paradaItem("evt-1", 10L, EstadoEntrega.ENTREGADO);
        SincronizacionParadaItemRequest b = paradaItem("evt-1", 10L, EstadoEntrega.ENTREGADO);
        SincronizacionParadasRequest req = SincronizacionParadasRequest.builder()
                .paradas(List.of(a, b))
                .build();

        SincronizacionParadasResponse resp = service.procesarParadasOffline(req, "test@test.com");

        verify(paradaProcessor, times(1)).procesarParada(any(), any());
        assertThat(resp.getProcesados()).hasSize(2);
        assertThat(resp.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Paradas: misma rutaPedidoId con UUID distinto en batch → segunda va a procesados")
    void paradas_mismaParadaDistintoUuidEnBatch_segundaVaaProcesados() {
        SincronizacionParadaItemRequest a = paradaItem("evt-1", 10L, EstadoEntrega.ENTREGADO);
        SincronizacionParadaItemRequest b = paradaItem("evt-2", 10L, EstadoEntrega.ENTREGADO);
        SincronizacionParadasRequest req = SincronizacionParadasRequest.builder()
                .paradas(List.of(a, b))
                .build();

        SincronizacionParadasResponse resp = service.procesarParadasOffline(req, "test@test.com");

        verify(paradaProcessor, times(1)).procesarParada(any(), any());
        assertThat(resp.getProcesados()).hasSize(2);
        assertThat(resp.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Paradas: parada ya entregada → se cuenta como procesado (idempotencia cross-batch)")
    void paradas_paradaYaEntregada_llegaOnline_seCuentaComoProcesado() {
        SincronizacionParadaItemRequest item = paradaItem("evt-1", 10L, EstadoEntrega.ENTREGADO);
        SincronizacionParadasRequest req = SincronizacionParadasRequest.builder()
                .paradas(List.of(item))
                .build();

        doThrow(new BusinessException(
                        String.format(Constantes.MSG_ESTADO_NO_CAMBIABLE, EstadoEntrega.ENTREGADO)))
                .when(paradaProcessor).procesarParada(any(), any());

        SincronizacionParadasResponse resp = service.procesarParadasOffline(req, "test@test.com");

        assertThat(resp.getProcesados()).hasSize(1);
        assertThat(resp.getProcesados().get(0).getUuidOffline()).isEqualTo("evt-1");
        assertThat(resp.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Paradas: rutaPedidoId no existe → va a errores con mensaje claro")
    void paradas_rutaPedidoNoExiste_vaErroresConMensajeClaro() {
        SincronizacionParadaItemRequest item = paradaItem("evt-1", 999L, EstadoEntrega.ENTREGADO);
        SincronizacionParadasRequest req = SincronizacionParadasRequest.builder()
                .paradas(List.of(item))
                .build();

        doThrow(new ResourceNotFoundException("Parada no encontrada"))
                .when(paradaProcessor).procesarParada(any(), any());

        SincronizacionParadasResponse resp = service.procesarParadasOffline(req, "test@test.com");

        assertThat(resp.getProcesados()).isEmpty();
        assertThat(resp.getErrores()).hasSize(1);
        assertThat(resp.getErrores().get(0).getUuidOffline()).isEqualTo("evt-1");
        assertThat(resp.getErrores().get(0).getRutaPedidoId()).isEqualTo(999L);
        assertThat(resp.getErrores().get(0).getError()).contains("Parada no encontrada");
    }

    @Test
    @DisplayName("Paradas: uuidOffline vacío → va a errores sin invocar processor")
    void paradas_uuidVacio_vaErrores() {
        SincronizacionParadaItemRequest item = paradaItem("", 10L, EstadoEntrega.ENTREGADO);
        SincronizacionParadasRequest req = SincronizacionParadasRequest.builder()
                .paradas(List.of(item))
                .build();

        SincronizacionParadasResponse resp = service.procesarParadasOffline(req, "test@test.com");

        assertThat(resp.getProcesados()).isEmpty();
        assertThat(resp.getErrores()).hasSize(1);
        assertThat(resp.getErrores().get(0).getError()).contains("uuidOffline es obligatorio");
        verifyNoInteractions(paradaProcessor);
    }

    private SincronizacionParadaItemRequest paradaItem(String uuid, Long rutaPedidoId, EstadoEntrega estado) {
        SincronizacionParadaItemRequest item = new SincronizacionParadaItemRequest();
        item.setUuidOffline(uuid);
        item.setRutaPedidoId(rutaPedidoId);
        item.setNuevoEstado(estado);
        return item;
    }

    @Test
    @DisplayName("Rutas: mismo uuidOffline en batch → segunda va a procesados sin rebotar")
    void rutas_mismoUuidEnBatch_segundaVaaProcesadosSinRebotar() {
        SincronizacionRutaItemRequest a = rutaItem("ruta-evt-1", 5L, EstadoRuta.EN_CURSO);
        SincronizacionRutaItemRequest b = rutaItem("ruta-evt-1", 5L, EstadoRuta.EN_CURSO);
        SincronizacionRutasRequest req = SincronizacionRutasRequest.builder()
                .cambios(List.of(a, b))
                .build();

        SincronizacionRutasResponse resp = service.procesarRutasOffline(req, "test@test.com");

        verify(rutaProcessor, times(1)).procesarRuta(any(), any());
        assertThat(resp.getProcesados()).hasSize(2);
        assertThat(resp.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Rutas: misma ruta con UUID distinto en batch → segunda va a procesados")
    void rutas_mismaRutaDistintoUuidEnBatch_segundaVaaProcesados() {
        SincronizacionRutaItemRequest a = rutaItem("ruta-evt-1", 5L, EstadoRuta.EN_CURSO);
        SincronizacionRutaItemRequest b = rutaItem("ruta-evt-2", 5L, EstadoRuta.EN_CURSO);
        SincronizacionRutasRequest req = SincronizacionRutasRequest.builder()
                .cambios(List.of(a, b))
                .build();

        SincronizacionRutasResponse resp = service.procesarRutasOffline(req, "test@test.com");

        verify(rutaProcessor, times(1)).procesarRuta(any(), any());
        assertThat(resp.getProcesados()).hasSize(2);
        assertThat(resp.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Rutas: cambio exitoso → va a procesados")
    void rutas_exitoso_vaProcesados() {
        SincronizacionRutaItemRequest item = rutaItem("ruta-evt-1", 5L, EstadoRuta.EN_CURSO);
        SincronizacionRutasRequest req = SincronizacionRutasRequest.builder()
                .cambios(List.of(item))
                .build();

        SincronizacionRutasResponse resp = service.procesarRutasOffline(req, "test@test.com");

        verify(rutaProcessor, times(1)).procesarRuta(any(), any());
        assertThat(resp.getProcesados()).hasSize(1);
        assertThat(resp.getProcesados().get(0).getUuidOffline()).isEqualTo("ruta-evt-1");
        assertThat(resp.getProcesados().get(0).getRutaId()).isEqualTo(5L);
        assertThat(resp.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Rutas: ruta no existe → va a errores con mensaje claro")
    void rutas_rutaNoExiste_vaErroresConMensajeClaro() {
        SincronizacionRutaItemRequest item = rutaItem("ruta-evt-1", 999L, EstadoRuta.EN_CURSO);
        SincronizacionRutasRequest req = SincronizacionRutasRequest.builder()
                .cambios(List.of(item))
                .build();

        doThrow(new ResourceNotFoundException("Ruta no encontrada"))
                .when(rutaProcessor).procesarRuta(any(), any());

        SincronizacionRutasResponse resp = service.procesarRutasOffline(req, "test@test.com");

        assertThat(resp.getProcesados()).isEmpty();
        assertThat(resp.getErrores()).hasSize(1);
        assertThat(resp.getErrores().get(0).getUuidOffline()).isEqualTo("ruta-evt-1");
        assertThat(resp.getErrores().get(0).getRutaId()).isEqualTo(999L);
        assertThat(resp.getErrores().get(0).getError()).contains("Ruta no encontrada");
    }

    @Test
    @DisplayName("Rutas: transición inválida → va a errores con mensaje claro")
    void rutas_transicionInvalida_vaErrores() {
        SincronizacionRutaItemRequest item = rutaItem("ruta-evt-1", 5L, EstadoRuta.COMPLETADA);
        SincronizacionRutasRequest req = SincronizacionRutasRequest.builder()
                .cambios(List.of(item))
                .build();

        doThrow(new BusinessException(
                        String.format(Constantes.MSG_TRANSICION_RUTA_INVALIDA,
                                EstadoRuta.COMPLETADA, EstadoRuta.COMPLETADA),
                        HttpStatus.BAD_REQUEST,
                        "TRANSICION_ESTADO_RUTA_INVALIDA"))
                .when(rutaProcessor).procesarRuta(any(), any());

        SincronizacionRutasResponse resp = service.procesarRutasOffline(req, "test@test.com");

        assertThat(resp.getProcesados()).isEmpty();
        assertThat(resp.getErrores()).hasSize(1);
        assertThat(resp.getErrores().get(0).getError()).contains("No se puede cambiar la ruta de COMPLETADA a COMPLETADA");
    }

    @Test
    @DisplayName("Rutas: uuidOffline vacío → va a errores sin invocar processor")
    void rutas_uuidVacio_vaErrores() {
        SincronizacionRutaItemRequest item = rutaItem("", 5L, EstadoRuta.EN_CURSO);
        SincronizacionRutasRequest req = SincronizacionRutasRequest.builder()
                .cambios(List.of(item))
                .build();

        SincronizacionRutasResponse resp = service.procesarRutasOffline(req, "test@test.com");

        assertThat(resp.getProcesados()).isEmpty();
        assertThat(resp.getErrores()).hasSize(1);
        assertThat(resp.getErrores().get(0).getError()).contains("uuidOffline es obligatorio");
        verifyNoInteractions(rutaProcessor);
    }

    private SincronizacionRutaItemRequest rutaItem(String uuid, Long rutaId, EstadoRuta estado) {
        SincronizacionRutaItemRequest item = new SincronizacionRutaItemRequest();
        item.setUuidOffline(uuid);
        item.setRutaId(rutaId);
        item.setNuevoEstado(estado);
        return item;
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