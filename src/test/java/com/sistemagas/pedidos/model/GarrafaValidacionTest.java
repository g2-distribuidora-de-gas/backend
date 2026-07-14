package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class GarrafaValidacionTest {

    @Test
    @DisplayName("@PrePersist: rechaza tipo null con BusinessException 400")
    void rechazarTipoNull() throws Exception {
        Garrafa garrafa = garrafaValida().tipo(null).build();

        BusinessException ex = catchThrowableOfType(
                () -> invokeValidar(garrafa), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().value()).isEqualTo(400);
        assertThat(ex.getCodigo()).isEqualTo("GARRAFA_TIPO_OBLIGATORIO");
    }

    @Test
    @DisplayName("@PrePersist: rechaza capacidadKg null con BusinessException 400")
    void rechazarCapacidadNull() throws Exception {
        Garrafa garrafa = garrafaValida().build();
        garrafa.setCapacidadKg(null);

        BusinessException ex = catchThrowableOfType(
                () -> invokeValidar(garrafa), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().value()).isEqualTo(400);
        assertThat(ex.getCodigo()).isEqualTo("GARRAFA_CAPACIDAD_OBLIGATORIA");
    }

    @Test
    @DisplayName("@PrePersist: rechaza capacidad inconsistente con el tipo")
    void rechazarCapacidadInconsistente() throws Exception {
        Garrafa garrafa = garrafaValida().tipo(TipoGarrafa.GARRAFA_10KG).capacidadKg(15).build();

        BusinessException ex = catchThrowableOfType(
                () -> invokeValidar(garrafa), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().value()).isEqualTo(400);
        assertThat(ex.getCodigo()).isEqualTo("GARRAFA_CAPACIDAD_INCONSISTENTE");
        assertThat(ex.getMessage()).contains("15kg").contains("GARRAFA_10KG");
    }

    @Test
    @DisplayName("@PrePersist: acepta garrafa valida (no lanza)")
    void aceptaGarrafaValida() throws Exception {
        Garrafa garrafa = garrafaValida().build();

        invokeValidar(garrafa);
    }

    private Garrafa.GarrafaBuilder garrafaValida() {
        return Garrafa.builder()
                .tipo(TipoGarrafa.GARRAFA_10KG)
                .capacidadKg(10)
                .precio(new BigDecimal("5000"))
                .stockDisponible(100)
                .activo(true);
    }

    private void invokeValidar(Garrafa garrafa) throws Exception {
        Method method = Garrafa.class.getDeclaredMethod("validarCapacidad");
        method.setAccessible(true);
        try {
            method.invoke(garrafa);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}