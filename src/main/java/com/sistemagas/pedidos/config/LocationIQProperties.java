package com.sistemagas.pedidos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "locationiq")
@Getter
@Setter
public class LocationIQProperties {
    private String apiKey;
    private String geocodingUrl = "https://us1.locationiq.com/v1/search.php";
    private String routingUrl = "https://us1.locationiq.com/v1/directions/driving/";
}
