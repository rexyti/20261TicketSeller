package com.ticketseller.infrastructure.adapter.in.scheduler.inventario;

import com.ticketseller.application.inventario.LiberarHoldsVencidosUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LiberacionHoldsScheduler {

    private final LiberarHoldsVencidosUseCase liberarHoldsVencidosUseCase;

    @Scheduled(fixedDelay = 60_000)
    public void liberarHoldsVencidos() {
        liberarHoldsVencidosUseCase.ejecutar(LocalDateTime.now())
                .subscribe();
    }
}
