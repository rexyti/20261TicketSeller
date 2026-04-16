package com.ticketseller.application.recinto;

import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarRecintosUseCaseTest {

    @Test
    void deberiaListarSoloActivos() {
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        ListarRecintosUseCase useCase = new ListarRecintosUseCase(repositoryPort);

        when(repositoryPort.listarTodos()).thenReturn(Flux.just(
                Recinto.builder().nombre("A").activo(true).build(),
                Recinto.builder().nombre("B").activo(false).build()
        ));

        StepVerifier.create(useCase.ejecutar())
                .expectNextMatches(recinto -> "A".equals(recinto.getNombre()))
                .verifyComplete();
    }
}

