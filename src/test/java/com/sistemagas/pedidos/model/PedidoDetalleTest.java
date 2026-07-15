package com.sistemagas.pedidos.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests para verificar el contrato de {@link PedidoDetalle#subtotal}:
 * se calcula UNA vez en @PrePersist y NO se recalcula en @PreUpdate.
 * Esto preserva el valor historico de venta aunque luego el repartidor
 * actualice {@code cantidadEntregada} (entrega parcial).
 */
class PedidoDetalleTest {

    @Test
    @DisplayName("@PrePersist calcularSubtotal: usa cantidad, NO cantidadEntregada")
    void prePersist_usaCantidadParaCalcularSubtotal() throws Exception {
        PedidoDetalle detalle = PedidoDetalle.builder()
                .garrafaId(1L)
                .cantidad(5)
                .cantidadEntregada(null)
                .precioUnitario(new BigDecimal("5500.00"))
                .build();

        invokeCalcular(detalle);

        assertThat(detalle.getSubtotal()).isEqualByComparingTo(new BigDecimal("27500.00"));
    }

    @Test
    @DisplayName("@PrePersist calcularSubtotal: ignora cantidadEntregada aunque este seteada")
    void prePersist_ignoraCantidadEntregadaInicial() throws Exception {
        PedidoDetalle detalle = PedidoDetalle.builder()
                .garrafaId(1L)
                .cantidad(5)
                .cantidadEntregada(2)
                .precioUnitario(new BigDecimal("5500.00"))
                .build();

        invokeCalcular(detalle);

        assertThat(detalle.getSubtotal()).isEqualByComparingTo(new BigDecimal("27500.00"));
    }

    @Test
    @DisplayName("subtotal NO se recalcula cuando cantidadEntregada cambia (entrega parcial)")
    void subtotalNoCambiaTrasEntregaParcial() throws Exception {
        PedidoDetalle detalle = PedidoDetalle.builder()
                .garrafaId(1L)
                .cantidad(5)
                .cantidadEntregada(null)
                .precioUnitario(new BigDecimal("5500.00"))
                .build();

        invokeCalcular(detalle);
        BigDecimal subtotalOriginal = detalle.getSubtotal();
        assertThat(subtotalOriginal).isEqualByComparingTo(new BigDecimal("27500.00"));

        detalle.setCantidadEntregada(3);
        detalle.setCantidadEntregada(0);
        detalle.setCantidadEntregada(null);
        detalle.setCantidad(99);
        detalle.setPrecioUnitario(new BigDecimal("99999.99"));

        assertThat(detalle.getSubtotal())
                .as("subtotal NO debe recalcularse aunque cambie cantidadEntregada, cantidad o precioUnitario")
                .isEqualByComparingTo(subtotalOriginal);
    }

    @Test
    @DisplayName("@PrePersist calcularSubtotal: precioUnitario null tira IllegalStateException (no calcula 0)")
    void prePersist_sinPrecioUnitario_lanzaExcepcion() throws Exception {
        PedidoDetalle detalle = PedidoDetalle.builder()
                .garrafaId(1L)
                .cantidad(5)
                .precioUnitario(null)
                .build();

        assertThatThrownBy(() -> invokeCalcular(detalle))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("precioUnitario");
    }

    private void invokeCalcular(PedidoDetalle detalle) throws Exception {
        Method m = PedidoDetalle.class.getDeclaredMethod("calcularSubtotal");
        m.setAccessible(true);
        try {
            m.invoke(detalle);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            // Desenvuelve para que el assert reciba la causa real del callback JPA.
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Exception e) throw e;
            throw ex;
        }
    }
}
