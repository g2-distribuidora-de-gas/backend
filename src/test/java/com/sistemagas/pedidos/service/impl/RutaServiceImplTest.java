package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.DepositoProperties;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.dto.response.RutaReprogramadaResponse;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import com.sistemagas.pedidos.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RutaServiceImplTest {

    @Mock private RutaRepository rutaRepository;
    @Mock private RutaPedidoRepository rutaPedidoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private com.sistemagas.pedidos.service.RoutingService routingService;
    @Mock private com.sistemagas.pedidos.service.InventarioService inventarioService;
    @Mock private com.sistemagas.pedidos.repository.DepositoRepository depositoRepository;
    @Mock private TipoGarrafaStockRepository tipoGarrafaStockRepository;
    @Mock private DepositoProperties depositoProperties;
    @Mock private SupabaseStorageService supabaseStorageService;
    @Mock private TrackingService trackingService;

    private RutaServiceImpl service;

    private Usuario repartidorDuenio;
    private Usuario otroRepartidor;
    private Usuario adminUser;
    private Cliente cliente;
    private Pedido pedido;
    private RutaPedido parada;

    @BeforeEach
    void setUp() {
        service = new RutaServiceImpl(rutaRepository, rutaPedidoRepository, pedidoRepository,
                usuarioRepository, routingService, inventarioService, depositoRepository, tipoGarrafaStockRepository, depositoProperties,
                supabaseStorageService, trackingService);

        repartidorDuenio = Usuario.builder().id(10L).rol(RolUsuario.REPARTIDOR).build();
        otroRepartidor = Usuario.builder().id(20L).rol(RolUsuario.REPARTIDOR).build();
        adminUser = Usuario.builder().id(99L).rol(RolUsuario.ADMIN).build();

        cliente = Cliente.builder()
                .id(7L)
                .nombre("Juan")
                .telefono("+5491112345678")
                .direccion("Av. Corrientes 1234")
                .latitud(new BigDecimal("-34.6037"))
                .longitud(new BigDecimal("-58.3816"))
                .fotoEvidenciaPath("cliente-7/foto.jpg")
                .build();

        PedidoDetalle detalle = PedidoDetalle.builder()
                .id(100L)
                .tipoGarrafaId(1L)
                .cantidad(2)
                .cantidadEntregada(null)
                .precioUnitario(new BigDecimal("5500.00"))
                .subtotal(new BigDecimal("11000.00"))
                .build();

        pedido = Pedido.builder()
                .id(42L)
                .uuidOffline("uuid-ped-1")
                .cliente(cliente)
                .direccionEntrega("Av. Corrientes 1234")
                .estado(EstadoPedido.EN_PROCESO)
                .detalles(new java.util.ArrayList<>(List.of(detalle)))
                .build();

        Ruta ruta = Ruta.builder()
                .id(1L)
                .repartidor(repartidorDuenio)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                .build();

        parada = RutaPedido.builder()
                .id(10L)
                .ruta(ruta)
                .pedido(pedido)
                .orden(1)
                .distanciaDesdeAnteriorM(0)
                .duracionDesdeAnteriorS(0)
                .estadoEntrega(EstadoEntrega.PENDIENTE)
                .build();

        Deposito depositoMock = new Deposito();
        depositoMock.setId(1L);
        lenient().when(depositoRepository.findByRepartidorIdAndActivoTrue(anyLong()))
                .thenReturn(Optional.of(depositoMock));

        TipoGarrafaStock tipoStockMock = new TipoGarrafaStock();
        tipoStockMock.setId(10L);
        tipoStockMock.setCodigo("10KG");
        lenient().when(tipoGarrafaStockRepository.findByCodigo(anyString()))
                .thenReturn(Optional.of(tipoStockMock));
                
        lenient().when(tipoGarrafaStockRepository.findById(anyLong()))
                .thenReturn(Optional.of(tipoGarrafaStockMock(1L, "10KG")));
    }

    private TipoGarrafaStock tipoGarrafaStockMock(Long id, String codigo) {
        TipoGarrafaStock tgs = new TipoGarrafaStock();
        tgs.setId(id);
        tgs.setCodigo(codigo);
        tgs.setPrecio(new BigDecimal("5500.00"));
        return tgs;
    }

    @Test
    @DisplayName("obtenerPedidoDeParada: REPARTIDOR duenio de la ruta recibe el DTO completo")
    void obtenerPedidoDeParada_repartidorDuenio_retornaDtoCompleto() {
        when(rutaPedidoRepository.findById(10L)).thenReturn(Optional.of(parada));
        when(tipoGarrafaStockRepository.findById(1L)).thenReturn(Optional.of(tipoGarrafaStockMock(1L, "10KG")));
        when(supabaseStorageService.getSignedUrl(any())).thenReturn("https://signed.example/cliente.jpg");

        DeliveryReadOnlyResponse response = service.obtenerPedidoDeParada(10L, repartidorDuenio);

        assertThat(response).isNotNull();
        assertThat(response.getPedidoId()).isEqualTo(42L);
        assertThat(response.getUuidOffline()).isEqualTo("uuid-ped-1");
        assertThat(response.getEstado()).isEqualTo(EstadoPedido.EN_PROCESO);
        assertThat(response.getCliente().getId()).isEqualTo(7L);
        assertThat(response.getCliente().getNombre()).isEqualTo("Juan");
        assertThat(response.getCliente().getUrlFotoEvidencia()).isEqualTo("https://signed.example/cliente.jpg");
        assertThat(response.getDetalles()).hasSize(1);
        assertThat(response.getDetalles().get(0).getGarrafaTipo())
                .isEqualTo("10KG");
        assertThat(response.getDetalles().get(0).getCantidad()).isEqualTo(2);
        assertThat(response.getDetalles().get(0).getCantidadEntregada()).isNull();
        assertThat(response.getParada().getRutaPedidoId()).isEqualTo(10L);
        assertThat(response.getParada().getEstadoEntrega()).isEqualTo(EstadoEntrega.PENDIENTE);
        assertThat(response.getParada().getMotivoFallo()).isNull();
    }

    @Test
    @DisplayName("obtenerPedidoDeParada: REPARTIDOR ajeno a la ruta recibe 403")
    void obtenerPedidoDeParada_repartidorAjeno_lanza403() {
        when(rutaPedidoRepository.findById(10L)).thenReturn(Optional.of(parada));

        assertThatThrownBy(() -> service.obtenerPedidoDeParada(10L, otroRepartidor))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No puedes ver pedidos de otro repartidor")
                .extracting("status").isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);

        verify(supabaseStorageService, never()).getSignedUrl(any());
    }

    @Test
    @DisplayName("obtenerPedidoDeParada: ADMIN puede ver paradas de cualquier repartidor")
    void obtenerPedidoDeParada_adminVeCualquierParada() {
        when(rutaPedidoRepository.findById(10L)).thenReturn(Optional.of(parada));
        when(tipoGarrafaStockRepository.findById(1L)).thenReturn(Optional.of(tipoGarrafaStockMock(1L, "10KG")));

        DeliveryReadOnlyResponse response = service.obtenerPedidoDeParada(10L, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getPedidoId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("obtenerPedidoDeParada: rutaPedidoId inexistente lanza 404")
    void obtenerPedidoDeParada_paradaNoExiste_lanza404() {
        when(rutaPedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPedidoDeParada(999L, repartidorDuenio))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parada no encontrada");

        verify(supabaseStorageService, never()).getSignedUrl(any());
        verify(tipoGarrafaStockRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("obtenerRutaActivaRepartidor: multiples rutas EN_CURSO devuelve la primera sin tirar")
    void obtenerRutaActivaRepartidor_multiplesEnCurso_devuelvePrimeraSinExplotar() {
        Ruta r1 = Ruta.builder().id(101L).estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO).repartidor(repartidorDuenio).build();
        Ruta r2 = Ruta.builder().id(102L).estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO).repartidor(repartidorDuenio).build();
        Ruta r3 = Ruta.builder().id(103L).estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO).repartidor(repartidorDuenio).build();
        when(rutaRepository.findByRepartidorIdAndFechaRepartoAndEstadoIn(
                eq(10L), any(), anyList()))
                .thenReturn(List.of(r1, r2, r3));

        Ruta result = service.obtenerRutaActivaRepartidor(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("obtenerRutaActivaRepartidor: sin rutas EN_CURSO cae a PLANIFICADAS")
    void obtenerRutaActivaRepartidor_sinEnCurso_caeAPlanificadas() {
        Ruta plan = Ruta.builder().id(201L).estado(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA).repartidor(repartidorDuenio).build();
        when(rutaRepository.findByRepartidorIdAndFechaRepartoAndEstadoIn(
                eq(10L), any(), anyList()))
                .thenReturn(List.of(plan));

        Ruta result = service.obtenerRutaActivaRepartidor(10L);

        assertThat(result.getId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("obtenerRutaActivaRepartidor: incluye REPROGRAMADA si no hay EN_CURSO ni PLANIFICADA")
    void obtenerRutaActivaRepartidor_incluyeReprogramadaEnPrioridad() {
        Ruta reprogramada = Ruta.builder().id(301L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA)
                .repartidor(repartidorDuenio)
                .build();
        when(rutaRepository.findByRepartidorIdAndFechaRepartoAndEstadoIn(
                eq(10L), any(), anyList()))
                .thenReturn(List.of(reprogramada));

        Ruta result = service.obtenerRutaActivaRepartidor(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(301L);
        assertThat(result.getEstado()).isEqualTo(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA);
    }

    @Test
    @DisplayName("cambiarEstadoRuta: PLANIFICADA → EN_CURSO es valida")
    void cambiarEstadoRuta_planificada_aEnCurso_ok() {
        Ruta ruta = Ruta.builder().id(5L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA)
                .repartidor(repartidorDuenio)
                .build();
        when(rutaRepository.findById(5L)).thenReturn(Optional.of(ruta));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));

        Ruta result = service.cambiarEstadoRuta(5L,
                com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO, repartidorDuenio);

        assertThat(result.getEstado()).isEqualTo(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO);
        verify(rutaRepository).save(any(Ruta.class));
    }

    @Test
    @DisplayName("cambiarEstadoRuta: COMPLETADA → EN_CURSO lanza BusinessException (estado terminal)")
    void cambiarEstadoRuta_completada_aEnCurso_lanzaBusinessException() {
        Ruta ruta = Ruta.builder().id(5L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.COMPLETADA)
                .repartidor(repartidorDuenio)
                .build();
        when(rutaRepository.findById(5L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> service.cambiarEstadoRuta(5L,
                        com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO, repartidorDuenio))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede cambiar la ruta de COMPLETADA a EN_CURSO");
        verify(rutaRepository, never()).save(any(Ruta.class));
    }

    @Test
    @DisplayName("cambiarEstadoRuta: PLANIFICADA → COMPLETADA directa lanza BusinessException (debe pasar por EN_CURSO)")
    void cambiarEstadoRuta_planificada_aCompletadaDirecta_lanzaBusinessException() {
        Ruta ruta = Ruta.builder().id(5L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA)
                .repartidor(repartidorDuenio)
                .build();
        when(rutaRepository.findById(5L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> service.cambiarEstadoRuta(5L,
                        com.sistemagas.pedidos.enums.EstadoRuta.COMPLETADA, repartidorDuenio))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede cambiar la ruta de PLANIFICADA a COMPLETADA");
        verify(rutaRepository, never()).save(any(Ruta.class));
    }

    @Test
    @DisplayName("cambiarEstadoRuta: CANCELADA → EN_CURSO lanza BusinessException (estado terminal)")
    void cambiarEstadoRuta_cancelada_aEnCurso_lanzaBusinessException() {
        Ruta ruta = Ruta.builder().id(5L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.CANCELADA)
                .repartidor(repartidorDuenio)
                .build();
        when(rutaRepository.findById(5L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> service.cambiarEstadoRuta(5L,
                        com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO, repartidorDuenio))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede cambiar la ruta de CANCELADA a EN_CURSO");
        verify(rutaRepository, never()).save(any(Ruta.class));
    }

    @Test
    @DisplayName("cambiarEstadoRuta: REPARTIDOR ajeno no puede cambiar ruta de otro")
    void cambiarEstadoRuta_repartidorAjeno_lanzaBusinessException() {
        Ruta ruta = Ruta.builder().id(5L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA)
                .repartidor(repartidorDuenio)
                .build();
        when(rutaRepository.findById(5L)).thenReturn(Optional.of(ruta));

        assertThatThrownBy(() -> service.cambiarEstadoRuta(5L,
                        com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO, otroRepartidor))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No puedes cambiar el estado de una ruta de otro repartidor");
    }

    @Test
    @DisplayName("actualizarEstadoParada: al procesar la ultima parada pendiente con todas las demas ENTREGADO, ruta queda COMPLETADA")
    void actualizarEstadoParada_100PorCientoEntregadas_quedaCompletada() {
        PedidoDetalle det = new PedidoDetalle();
        det.setId(1L);
        det.setTipoGarrafaId(1L);
        det.setCantidad(2);
        det.setPrecioUnitario(new BigDecimal("5500"));

        Pedido ped = Pedido.builder()
                .id(10L)
                .estado(EstadoPedido.EN_PROCESO)
                .detalles(new java.util.ArrayList<>(List.of(det)))
                .build();

        RutaPedido parada1 = RutaPedido.builder()
                .id(100L)
                .pedido(ped)
                .orden(1)
                .estadoEntrega(EstadoEntrega.ENTREGADO)
                .build();
        RutaPedido parada2 = RutaPedido.builder()
                .id(101L)
                .pedido(ped)
                .orden(2)
                .estadoEntrega(EstadoEntrega.PENDIENTE)
                .build();

        Ruta ruta = Ruta.builder()
                .id(1L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                .repartidor(repartidorDuenio)
                .paradas(new java.util.ArrayList<>(List.of(parada1, parada2)))
                .build();
        parada1.setRuta(ruta);
        parada2.setRuta(ruta);

        when(rutaPedidoRepository.findById(101L)).thenReturn(Optional.of(parada2));
        when(rutaPedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));

        com.sistemagas.pedidos.dto.request.ActualizarParadaRequest req =
                new com.sistemagas.pedidos.dto.request.ActualizarParadaRequest();
        req.setNuevoEstado(EstadoEntrega.ENTREGADO);
        service.actualizarEstadoParada(101L, req, repartidorDuenio);

        verify(rutaRepository).save(argThat(r -> r.getEstado()
                == com.sistemagas.pedidos.enums.EstadoRuta.COMPLETADA));
    }

    @Test
    @DisplayName("actualizarEstadoParada: al procesar la ultima PENDIENTE con todas las demas FALLIDO, ruta queda REPROGRAMADA (no COMPLETADA)")
    void actualizarEstadoParada_100PorCientoFallidas_quedaReprogramada() {
        PedidoDetalle det = new PedidoDetalle();
        det.setId(1L);
        det.setTipoGarrafaId(1L);
        det.setCantidad(2);
        det.setPrecioUnitario(new BigDecimal("5500"));

        Pedido ped = Pedido.builder()
                .id(10L)
                .estado(EstadoPedido.EN_PROCESO)
                .detalles(new java.util.ArrayList<>(List.of(det)))
                .build();

        RutaPedido parada1 = RutaPedido.builder()
                .id(100L)
                .pedido(ped)
                .orden(1)
                .estadoEntrega(EstadoEntrega.FALLIDO)
                .build();
        RutaPedido parada2 = RutaPedido.builder()
                .id(101L)
                .pedido(ped)
                .orden(2)
                .estadoEntrega(EstadoEntrega.PENDIENTE)
                .build();

        Ruta ruta = Ruta.builder()
                .id(1L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                .repartidor(repartidorDuenio)
                .paradas(new java.util.ArrayList<>(List.of(parada1, parada2)))
                .build();
        parada1.setRuta(ruta);
        parada2.setRuta(ruta);

        when(rutaPedidoRepository.findById(101L)).thenReturn(Optional.of(parada2));
        when(rutaPedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));

        com.sistemagas.pedidos.dto.request.ActualizarParadaRequest req =
                new com.sistemagas.pedidos.dto.request.ActualizarParadaRequest();
        req.setNuevoEstado(EstadoEntrega.FALLIDO);
        req.setMotivoFallo("cliente ausente");
        service.actualizarEstadoParada(101L, req, repartidorDuenio);

        verify(rutaRepository).save(argThat(r -> r.getEstado()
                == com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA));
    }

    @Test
    @DisplayName("actualizarEstadoParada: mixto (ENTREGADO + FALLIDO) → ruta queda COMPLETADA")
    void actualizarEstadoParada_mixto_quedaCompletada() {
        PedidoDetalle det = new PedidoDetalle();
        det.setId(1L);
        det.setTipoGarrafaId(1L);
        det.setCantidad(2);
        det.setPrecioUnitario(new BigDecimal("5500"));

        Pedido ped = Pedido.builder()
                .id(10L)
                .estado(EstadoPedido.EN_PROCESO)
                .detalles(new java.util.ArrayList<>(List.of(det)))
                .build();

        RutaPedido parada1 = RutaPedido.builder()
                .id(100L)
                .pedido(ped)
                .orden(1)
                .estadoEntrega(EstadoEntrega.ENTREGADO)
                .build();
        RutaPedido parada2 = RutaPedido.builder()
                .id(101L)
                .pedido(ped)
                .orden(2)
                .estadoEntrega(EstadoEntrega.PENDIENTE)
                .build();

        Ruta ruta = Ruta.builder()
                .id(1L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                .repartidor(repartidorDuenio)
                .paradas(new java.util.ArrayList<>(List.of(parada1, parada2)))
                .build();
        parada1.setRuta(ruta);
        parada2.setRuta(ruta);

        when(rutaPedidoRepository.findById(101L)).thenReturn(Optional.of(parada2));
        when(rutaPedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(inv -> inv.getArgument(0));

        com.sistemagas.pedidos.dto.request.ActualizarParadaRequest req =
                new com.sistemagas.pedidos.dto.request.ActualizarParadaRequest();
        req.setNuevoEstado(EstadoEntrega.FALLIDO);
        req.setMotivoFallo("cliente ausente");
        service.actualizarEstadoParada(101L, req, repartidorDuenio);

        verify(rutaRepository).save(argThat(r -> r.getEstado()
                == com.sistemagas.pedidos.enums.EstadoRuta.COMPLETADA));
    }

    @Test
    @DisplayName("actualizarEstadoParada: queda otra PENDIENTE → no se cambia estado de la ruta")
    void actualizarEstadoParada_quedaUnaPendiente_noCambiaEstadoRuta() {
        PedidoDetalle det = new PedidoDetalle();
        det.setId(1L);
        det.setTipoGarrafaId(1L);
        det.setCantidad(2);
        det.setPrecioUnitario(new BigDecimal("5500"));

        Pedido ped = Pedido.builder()
                .id(10L)
                .estado(EstadoPedido.EN_PROCESO)
                .detalles(new java.util.ArrayList<>(List.of(det)))
                .build();

        RutaPedido parada1 = RutaPedido.builder()
                .id(100L)
                .pedido(ped)
                .orden(1)
                .estadoEntrega(EstadoEntrega.PENDIENTE)
                .build();
        RutaPedido parada2 = RutaPedido.builder()
                .id(101L)
                .pedido(ped)
                .orden(2)
                .estadoEntrega(EstadoEntrega.PENDIENTE)
                .build();

        Ruta ruta = Ruta.builder()
                .id(1L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)
                .repartidor(repartidorDuenio)
                .paradas(new java.util.ArrayList<>(List.of(parada1, parada2)))
                .build();
        parada1.setRuta(ruta);
        parada2.setRuta(ruta);

        when(rutaPedidoRepository.findById(100L)).thenReturn(Optional.of(parada1));
        when(rutaPedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        com.sistemagas.pedidos.dto.request.ActualizarParadaRequest req =
                new com.sistemagas.pedidos.dto.request.ActualizarParadaRequest();
        req.setNuevoEstado(EstadoEntrega.ENTREGADO);
        service.actualizarEstadoParada(100L, req, repartidorDuenio);

        verify(rutaRepository, never()).save(any(Ruta.class));
    }

    @Test
    @DisplayName("listarReprogramadas: sin filtros delega al repo con defaults (ultimos 30 dias, estado REPROGRAMADA)")
    void listarReprogramadas_sinFiltros_delegaAlRepoConDefaults() {
        LocalDate desdeEsperado = LocalDate.now().minusDays(30);
        LocalDate hastaEsperado = LocalDate.now();

        when(rutaRepository.findByEstadoAndFechaRepartoBetween(
                eq(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA),
                eq(desdeEsperado), eq(hastaEsperado), any()))
                .thenReturn(List.of());

        List<RutaReprogramadaResponse> resp = service.listarReprogramadas(
                null, null, null, null, null);

        assertThat(resp).isEmpty();
        verify(rutaRepository).findByEstadoAndFechaRepartoBetween(
                eq(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA),
                eq(desdeEsperado), eq(hastaEsperado), any());
        verify(rutaRepository, never())
                .findByRepartidorIdAndEstadoAndFechaRepartoBetween(anyLong(), any(), any(), any(), any());
        verify(rutaRepository, never())
                .findByEstadoAndUpdatedAtGreaterThan(any(), any(), any());
    }

    @Test
    @DisplayName("listarReprogramadas: con repartidorId delega al repo especifico de repartidor")
    void listarReprogramadas_conRepartidorId_delegaAlRepoEspecifico() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        when(rutaRepository.findByRepartidorIdAndEstadoAndFechaRepartoBetween(
                eq(10L), eq(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA),
                eq(desde), eq(hasta), any()))
                .thenReturn(List.of());

        List<RutaReprogramadaResponse> resp = service.listarReprogramadas(
                desde, hasta, 10L, null, 50);

        assertThat(resp).isEmpty();
        verify(rutaRepository).findByRepartidorIdAndEstadoAndFechaRepartoBetween(
                eq(10L), eq(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA),
                eq(desde), eq(hasta), any());
    }

    @Test
    @DisplayName("listarReprogramadas: con minUpdatedAt (sin repartidor) delega al repo por updatedAt")
    void listarReprogramadas_conMinUpdatedAt_delegaAlRepoEspecifico() {
        Instant min = Instant.parse("2026-07-01T00:00:00Z");

        when(rutaRepository.findByEstadoAndUpdatedAtGreaterThan(
                eq(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA), eq(min), any()))
                .thenReturn(List.of());

        List<RutaReprogramadaResponse> resp = service.listarReprogramadas(
                null, null, null, min, null);

        assertThat(resp).isEmpty();
        verify(rutaRepository).findByEstadoAndUpdatedAtGreaterThan(
                eq(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA), eq(min), any());
    }

    @Test
    @DisplayName("listarReprogramadas: sin resultados retorna lista vacia (no 404)")
    void listarReprogramadas_sinResultados_retornaListaVacia() {
        when(rutaRepository.findByEstadoAndFechaRepartoBetween(any(), any(), any(), any()))
                .thenReturn(List.of());

        List<RutaReprogramadaResponse> resp = service.listarReprogramadas(
                LocalDate.now().minusDays(7), LocalDate.now(), null, null, 100);

        assertThat(resp).isNotNull();
        assertThat(resp).isEmpty();
    }

    @Test
    @DisplayName("listarReprogramadas: mapea solo paradas FALLIDO y calcula metricas correctamente")
    void listarReprogramadas_mapeaSoloFallidas_conMetricasCorrectas() {
        PedidoDetalle det1 = new PedidoDetalle();
        det1.setId(1L);
        det1.setCantidad(2);
        det1.setCantidadEntregada(0);
        PedidoDetalle det2 = new PedidoDetalle();
        det2.setId(2L);
        det2.setCantidad(3);
        det2.setCantidadEntregada(3);

        Pedido pedidoFallido = Pedido.builder()
                .id(10L)
                .uuidOffline("uuid-fall-1")
                .estado(EstadoPedido.REPROGRAMADO)
                .cliente(cliente)
                .detalles(new java.util.ArrayList<>(List.of(det1)))
                .build();
        Pedido pedidoEntregado = Pedido.builder()
                .id(11L)
                .uuidOffline("uuid-ent-1")
                .estado(EstadoPedido.ENTREGADO)
                .cliente(cliente)
                .detalles(new java.util.ArrayList<>(List.of(det2)))
                .build();

        RutaPedido paradaFallida1 = RutaPedido.builder()
                .id(100L).pedido(pedidoFallido).orden(1)
                .estadoEntrega(EstadoEntrega.FALLIDO).motivoFallo("Cliente ausente").build();
        RutaPedido paradaFallida2 = RutaPedido.builder()
                .id(101L).pedido(pedidoFallido).orden(2)
                .estadoEntrega(EstadoEntrega.FALLIDO).motivoFallo("No atiende telefono").build();
        RutaPedido paradaEntregada = RutaPedido.builder()
                .id(102L).pedido(pedidoEntregado).orden(3)
                .estadoEntrega(EstadoEntrega.ENTREGADO).build();

        Ruta ruta = Ruta.builder()
                .id(5L)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA)
                .repartidor(repartidorDuenio)
                .fechaReparto(LocalDate.of(2026, 7, 15))
                .paradas(new java.util.ArrayList<>(List.of(paradaFallida1, paradaFallida2, paradaEntregada)))
                .build();
        paradaFallida1.setRuta(ruta);
        paradaFallida2.setRuta(ruta);
        paradaEntregada.setRuta(ruta);

        when(rutaRepository.findByEstadoAndFechaRepartoBetween(any(), any(), any(), any()))
                .thenReturn(List.of(ruta));

        List<RutaReprogramadaResponse> resp = service.listarReprogramadas(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, 100);

        assertThat(resp).hasSize(1);
        RutaReprogramadaResponse item = resp.get(0);
        assertThat(item.getRutaId()).isEqualTo(5L);
        assertThat(item.getEstado()).isEqualTo(com.sistemagas.pedidos.enums.EstadoRuta.REPROGRAMADA);
        assertThat(item.getTotalParadas()).isEqualTo(3);
        assertThat(item.getTotalFallidas()).isEqualTo(2);
        assertThat(item.getTotalEntregadas()).isEqualTo(1);
        assertThat(item.getTotalPendientes()).isEqualTo(0);

        assertThat(item.getParadasFallidas()).hasSize(2);
        assertThat(item.getParadasFallidas())
                .extracting(com.sistemagas.pedidos.dto.response.ParadaFalloResponse::getRutaPedidoId)
                .containsExactly(100L, 101L);
        assertThat(item.getParadasFallidas().get(0).getMotivoFallo()).isEqualTo("Cliente ausente");
        assertThat(item.getParadasFallidas().get(1).getMotivoFallo()).isEqualTo("No atiende telefono");

        assertThat(item.getRepartidor().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("listarTodas: sin filtros delega al repo general con defaults (hoy-30, hoy, limit 100)")
    void listarTodas_sinFiltros_delegaAlRepoGeneralConDefaults() {
        LocalDate desdeEsperado = LocalDate.now().minusDays(30);
        LocalDate hastaEsperado = LocalDate.now();

        when(rutaRepository.findByFechaRepartoBetween(eq(desdeEsperado), eq(hastaEsperado), any()))
                .thenReturn(List.of());

        List<Ruta> resp = service.listarTodas(null, null, null, null);

        assertThat(resp).isEmpty();
        verify(rutaRepository).findByFechaRepartoBetween(eq(desdeEsperado), eq(hastaEsperado), any());
        verify(rutaRepository, never())
                .findByRepartidorIdAndFechaRepartoBetween(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("listarTodas: con repartidorId delega al repo especifico de repartidor")
    void listarTodas_conRepartidorId_delegaAlRepoEspecifico() {
        LocalDate desde = LocalDate.of(2026, 7, 1);
        LocalDate hasta = LocalDate.of(2026, 7, 31);

        Ruta r1 = Ruta.builder().id(1L).repartidor(repartidorDuenio)
                .estado(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA).build();
        when(rutaRepository.findByRepartidorIdAndFechaRepartoBetween(
                eq(10L), eq(desde), eq(hasta), any()))
                .thenReturn(List.of(r1));

        List<Ruta> resp = service.listarTodas(desde, hasta, 10L, 50);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getId()).isEqualTo(1L);
        verify(rutaRepository).findByRepartidorIdAndFechaRepartoBetween(
                eq(10L), eq(desde), eq(hasta), any());
        verify(rutaRepository, never())
                .findByFechaRepartoBetween(any(), any(), any());
    }

    @Test
    @DisplayName("listarTodas: limit null cae a 100 y limit <= 0 tambien cae a 100")
    void listarTodas_limitInvalido_caeADefault100() {
        when(rutaRepository.findByFechaRepartoBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.listarTodas(null, null, null, null);
        service.listarTodas(null, null, null, 0);
        service.listarTodas(null, null, null, -5);

        verify(rutaRepository, times(3))
                .findByFechaRepartoBetween(any(), any(), argThat(p ->
                        p != null && p.getPageSize() == 100));
    }
}
