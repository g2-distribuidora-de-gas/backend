package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.service.ClienteFotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteFotoCleanupJob {

    private final ClienteFotoService clienteFotoService;

    /**
     * Borra imagenes de cliente pendientes con mas de 7 dias y los archivos huerfanos en Supabase.
     * Corre todos los dias a las 3am.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void limpiarHuerfanos() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        log.info("Ejecutando limpieza de fotos de cliente pendientes con uploadedAt < {}", cutoff);
        clienteFotoService.limpiarHuerfanos(cutoff);
    }
}
