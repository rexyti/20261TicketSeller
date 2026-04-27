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
import static org.mockito.Mockito.when;

class VerificarDisponibilidadUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private VerificarDisponibilidadUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new VerificarDisponibilidadUseCase(asientoRepositoryPort);
    }

    @Test
    void cuandoAsientoDisponibleRetornaDisponibleTrue() {
        UUID asientoId = UUID.randomUUID();
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.DISPONIBLE)
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .expectNextMatches(response -> response.disponible() && response.mensaje() == null)
                .verifyComplete();
    }

    @Test
    void cuandoAsientoNoDisponibleRetornaDisponibleFalse() {
        UUID asientoId = UUID.randomUUID();
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.OCUPADO)
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .expectNextMatches(response -> !response.disponible()
                        && "ASIENTO NO DISPONIBLE".equals(response.mensaje()))
                .verifyComplete();
    }

    @Test
    void cuandoAsientoNoExisteRetornaError() {
        UUID asientoId = UUID.randomUUID();
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(asientoId))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
