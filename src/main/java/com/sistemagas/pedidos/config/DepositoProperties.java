package com.sistemagas.pedidos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.routing.deposito")
public class DepositoProperties {

    private String lat = "-26.2072404";
    private String lng = "-58.2123249";
}