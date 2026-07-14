package com.sistemagas.pedidos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DepositoProperties.class)
public class DepositoConfig {
}