package com.ticketseller.infrastructure.adapter.in.scheduler.checkout;

import com.ticketseller.application.checkout.LiberarReservaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiberacionReservasExpiradasScheduler {

    private final LiberarReservaUseCase liberarReservaUseCase;

    @Scheduled(fixedDelay = 60_000)
    public void liberarReservasExpiradas() {
        liberarReservaUseCase.ejecutar()
                .subscribe();
    }
}
