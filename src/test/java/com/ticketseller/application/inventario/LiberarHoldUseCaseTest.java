package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiberarHoldUseCaseTest {
    private final AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
    private final LiberarHoldUseCase useCase = new LiberarHoldUseCase(asientoRepositoryPort);

    @Test
    void liberaHoldExitosamente() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.liberarHold(id)).thenReturn(Mono.just(disponible));

        StepVerifier.create(useCase.ejecutar(id))
                .assertNext(a -> Assertions.assertEquals(EstadoAsiento.DISPONIBLE, a.getEstado()))
                .verifyComplete();
    }

    @Test
    void fallaConAsientoNoDisponibleAlLiberarAsientoInexistente() {
        UUID id = UUID.randomUUID();
        when(asientoRepositoryPort.liberarHold(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }
}
