package com.ticketseller.infrastructure.adapter.in.scheduler.conciliacion;

import com.ticketseller.application.conciliacion.ExpirarTransaccionesPendientesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ExpiracionTransaccionesScheduler {

    private static final long VENTANA_EXPIRACION_MINUTOS = 15;

    private final ExpirarTransaccionesPendientesUseCase expirarTransaccionesPendientesUseCase;

    @Scheduled(fixedDelay = 60_000)
    public void expirarPendientes() {
        LocalDateTime fechaCorte = LocalDateTime.now().minusMinutes(VENTANA_EXPIRACION_MINUTOS);
        expirarTransaccionesPendientesUseCase.ejecutar(fechaCorte)
                .subscribe();
    }
}
