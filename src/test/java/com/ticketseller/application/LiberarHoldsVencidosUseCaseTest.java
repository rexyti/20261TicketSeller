package com.ticketseller.application;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiberarHoldsVencidosUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private LiberarHoldsVencidosUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new LiberarHoldsVencidosUseCase(asientoRepositoryPort);
    }

    @Test
    void liberaTodosLosHoldsVencidos() {
        UUID asientoId1 = UUID.randomUUID();
        UUID asientoId2 = UUID.randomUUID();
        Asiento asiento1 = Asiento.builder().id(asientoId1).expiraEn(LocalDateTime.now().minusMinutes(1)).build();
        Asiento asiento2 = Asiento.builder().id(asientoId2).expiraEn(LocalDateTime.now().minusMinutes(2)).build();

        when(asientoRepositoryPort.findHoldsVencidos(any(LocalDateTime.class)))
                .thenReturn(Flux.just(asiento1, asiento2));
        when(asientoRepositoryPort.liberarHold(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.liberarHold(asientoId2)).thenReturn(Mono.just(asiento2));

        StepVerifier.create(useCase.ejecutar())
                .verifyComplete();

        verify(asientoRepositoryPort, times(1)).liberarHold(asientoId1);
        verify(asientoRepositoryPort, times(1)).liberarHold(asientoId2);
    }
}
