package com.sistemagas.pedidos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepositoPropertiesTest {

    @Test
    @DisplayName("Defaults apuntan a Formosa, Argentina")
    void defaults() {
        DepositoProperties props = new DepositoProperties();

        assertThat(props.getLat()).isEqualTo("-26.2072404");
        assertThat(props.getLng()).isEqualTo("-58.2123249");
    }

    @Test
    @DisplayName("Permite setear coordenadas custom")
    void customCoordinates() {
        DepositoProperties props = new DepositoProperties();
        props.setLat("-34.6037");
        props.setLng("-58.3816");

        assertThat(props.getLat()).isEqualTo("-34.6037");
        assertThat(props.getLng()).isEqualTo("-58.3816");
    }
}