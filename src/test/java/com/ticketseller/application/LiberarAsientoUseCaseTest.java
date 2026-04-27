package com.ticketseller.application;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiberarAsientoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private LiberarAsientoUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new LiberarAsientoUseCase(asientoRepositoryPort);
    }

    @Test
    void liberaHoldDelAsientoExitosamente() {
        UUID asientoId = UUID.randomUUID();
        Asiento asientoLiberado = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.DISPONIBLE)
                .expiraEn(null)
                .build();

        when(asientoRepositoryPort.liberarHold(asientoId)).thenReturn(Mono.just(asientoLiberado));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .verifyComplete();

        verify(asientoRepositoryPort).liberarHold(asientoId);
    }

    @Test
    void propagaErrorSiRepositorioFalla() {
        UUID asientoId = UUID.randomUUID();

        when(asientoRepositoryPort.liberarHold(asientoId))
                .thenReturn(Mono.error(new RuntimeException("Error de BD")));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .expectError(RuntimeException.class)
                .verify();
    }
}
