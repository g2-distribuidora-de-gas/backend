package com.sistemagas.pedidos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
@Component("supabaseConnection")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final int VALIDATION_TIMEOUT_SECONDS = 3;
    private static final String VALIDATION_QUERY = "SELECT 1";

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        long startMs = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(VALIDATION_QUERY)) {
            stmt.setQueryTimeout(VALIDATION_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean ok = rs.next() && rs.getInt(1) == 1;
                long latencyMs = System.currentTimeMillis() - startMs;
                if (ok) {
                    return Health.up()
                            .withDetail("validationQuery", VALIDATION_QUERY)
                            .withDetail("latencyMs", latencyMs)
                            .build();
                }
                return Health.down()
                        .withDetail("motivo", "La consulta de validacion no devolvio resultado esperado")
                        .withDetail("latencyMs", latencyMs)
                        .build();
            }
        } catch (Exception ex) {
            long latencyMs = System.currentTimeMillis() - startMs;
            log.error("Health check de base de datos fallo en {}ms: {}", latencyMs, ex.getMessage());
            return Health.down(ex)
                    .withDetail("latencyMs", latencyMs)
                    .withDetail("errorClass", ex.getClass().getSimpleName())
                    .build();
        }
    }
}