package com.ticketseller.infrastructure.adapter.in.scheduler;

import com.ticketseller.application.LiberarHoldsVencidosUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LiberacionHoldsScheduler {

    private static final Logger log = LoggerFactory.getLogger(LiberacionHoldsScheduler.class);
    private final LiberarHoldsVencidosUseCase liberarHoldsVencidosUseCase;

    public LiberacionHoldsScheduler(LiberarHoldsVencidosUseCase liberarHoldsVencidosUseCase) {
        this.liberarHoldsVencidosUseCase = liberarHoldsVencidosUseCase;
    }

    @Scheduled(fixedRate = 60000)
    public void liberarHolds() {
        log.info("Iniciando tarea programada: liberacion de holds vencidos");
        liberarHoldsVencidosUseCase.ejecutar()
                .doOnSuccess(v -> log.info("Finalizo tarea programada: liberacion de holds vencidos"))
                .doOnError(e -> log.error("Error en tarea programada de liberacion de holds", e))
                .subscribe();
    }
}
