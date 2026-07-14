package com.sistemagas.pedidos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigValidatorTest {

    private final SecurityConfigValidator validator = new SecurityConfigValidator();
    private final SpringApplication application = new SpringApplication();

    @Test
    @DisplayName("En perfil dev no valida nada (pasa con placeholder)")
    void devProfile_skipsValidation() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        env.setProperty("app.jwt.secret", "clave-secreta-de-desarrollo-cambiar-en-produccion-min-64-chars-!!!!");

        validator.postProcessEnvironment(env, application);

        assertThat(env.getProperty("app.jwt.secret")).isNotBlank();
    }

    @Test
    @DisplayName("En perfil prod con placeholder falla el arranque")
    void prodProfile_withPlaceholder_fails() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("app.jwt.secret", "clave-secreta-de-desarrollo-cambiar-en-produccion-min-64-chars-!!!!");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    @DisplayName("En perfil prod con secret corto falla el arranque")
    void prodProfile_withShortSecret_fails() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("app.jwt.secret", "corto");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demasiado corto");
    }

    @Test
    @DisplayName("En perfil prod con secret vacio falla el arranque")
    void prodProfile_withEmptySecret_fails() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("app.jwt.secret", "");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, application))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no esta configurado");
    }

    @Test
    @DisplayName("En perfil prod con secret valido (>=64 chars) pasa")
    void prodProfile_withValidSecret_passes() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("app.jwt.secret", "a".repeat(64));

        validator.postProcessEnvironment(env, application);

        assertThat(env.getProperty("app.jwt.secret")).hasSize(64);
    }
}