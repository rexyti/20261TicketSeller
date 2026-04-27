package com.ticketseller.infrastructure.adapter.in.scheduler;

import com.ticketseller.application.LiberarHoldsVencidosUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiberacionHoldsSchedulerTest {

    private LiberarHoldsVencidosUseCase liberarHoldsVencidosUseCase;
    private LiberacionHoldsScheduler scheduler;

    @BeforeEach
    void setUp() {
        liberarHoldsVencidosUseCase = mock(LiberarHoldsVencidosUseCase.class);
        scheduler = new LiberacionHoldsScheduler(liberarHoldsVencidosUseCase);
    }

    @Test
    void liberarHoldsInvocaUseCase() {
        when(liberarHoldsVencidosUseCase.ejecutar()).thenReturn(Mono.empty());

        scheduler.liberarHolds();

        verify(liberarHoldsVencidosUseCase).ejecutar();
    }

    @Test
    void liberarHoldsNoFallaSiUseCaseLanzaError() {
        when(liberarHoldsVencidosUseCase.ejecutar())
                .thenReturn(Mono.error(new RuntimeException("Error simulado")));

        // No debe lanzar excepción — el subscribe con doOnError la maneja
        scheduler.liberarHolds();

        verify(liberarHoldsVencidosUseCase).ejecutar();
    }
}
