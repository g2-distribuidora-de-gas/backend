package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.config.DepositoProperties;
import com.sistemagas.pedidos.dto.response.DeliveryReadOnlyResponse;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.enums.RolUsuario;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.Cliente;
import com.sistemagas.pedidos.model.Garrafa;
import com.sistemagas.pedidos.model.GarrafaModel;
import com.sistemagas.pedidos.model.Pedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PedidoRepository;
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.RutaRepository;
import com.sistemagas.pedidos.repository.UsuarioRepository;
import com.sistemagas.pedidos.repository.port.GarrafaRepositoryPort;
import com.sistemagas.pedidos.service.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    @Mock private com.sistemagas.pedidos.util.GarrafaStockHelper garrafaStockHelper;
    @Mock private DepositoProperties depositoProperties;
    @Mock private GarrafaRepositoryPort garrafaRepositoryPort;
    @Mock private SupabaseStorageService supabaseStorageService;

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
                usuarioRepository, routingService, garrafaStockHelper, depositoProperties,
                garrafaRepositoryPort, supabaseStorageService);

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
                .garrafaId(1L)
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
    }

    private Garrafa garrafaMock(Long id, com.sistemagas.pedidos.enums.TipoGarrafa tipo) {
        Integer[] stock = {100};
        return new Garrafa() {
            @Override public Long getId() { return id; }
            @Override public Integer getStockDisponible() { return stock[0]; }
            @Override public void setStockDisponible(Integer s) { stock[0] = s; }
            @Override public BigDecimal getPrecio() { return new BigDecimal("5500.00"); }
            @Override public com.sistemagas.pedidos.enums.TipoGarrafa getTipo() { return tipo; }
        };
    }

    @Test
    @DisplayName("obtenerPedidoDeParada: REPARTIDOR duenio de la ruta recibe el DTO completo")
    void obtenerPedidoDeParada_repartidorDuenio_retornaDtoCompleto() {
        when(rutaPedidoRepository.findById(10L)).thenReturn(Optional.of(parada));
        when(garrafaRepositoryPort.findById(1L)).thenReturn(Optional.of(garrafaMock(1L, com.sistemagas.pedidos.enums.TipoGarrafa.GARRAFA_10KG)));
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
                .isEqualTo(com.sistemagas.pedidos.enums.TipoGarrafa.GARRAFA_10KG);
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
        when(garrafaRepositoryPort.findById(1L)).thenReturn(Optional.of(garrafaMock(1L, com.sistemagas.pedidos.enums.TipoGarrafa.GARRAFA_10KG)));

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
        verify(garrafaRepositoryPort, never()).findById(anyLong());
    }

    @Test
    @DisplayName("obtenerRutaActivaRepartidor: multiples rutas EN_CURSO devuelve la primera sin tirar")
    void obtenerRutaActivaRepartidor_multiplesEnCurso_devuelvePrimeraSinExplotar() {
        Ruta r1 = Ruta.builder().id(101L).estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO).repartidor(repartidorDuenio).build();
        Ruta r2 = Ruta.builder().id(102L).estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO).repartidor(repartidorDuenio).build();
        Ruta r3 = Ruta.builder().id(103L).estado(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO).repartidor(repartidorDuenio).build();
        when(rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                eq(10L), any(), eq(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)))
                .thenReturn(List.of(r1, r2, r3));

        Ruta result = service.obtenerRutaActivaRepartidor(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("obtenerRutaActivaRepartidor: sin rutas EN_CURSO cae a PLANIFICADAS")
    void obtenerRutaActivaRepartidor_sinEnCurso_caeAPlanificadas() {
        Ruta plan = Ruta.builder().id(201L).estado(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA).repartidor(repartidorDuenio).build();
        when(rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                eq(10L), any(), eq(com.sistemagas.pedidos.enums.EstadoRuta.EN_CURSO)))
                .thenReturn(List.of());
        when(rutaRepository.findByRepartidorIdAndFechaRepartoAndEstado(
                eq(10L), any(), eq(com.sistemagas.pedidos.enums.EstadoRuta.PLANIFICADA)))
                .thenReturn(List.of(plan));

        Ruta result = service.obtenerRutaActivaRepartidor(10L);

        assertThat(result.getId()).isEqualTo(201L);
    }
}
