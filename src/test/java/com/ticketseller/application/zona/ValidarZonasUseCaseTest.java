package com.ticketseller.application.zona;

import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidarZonasUseCaseTest {

    @Test
    void deberiaRetornarTrueCuandoNoExcede() {
        UUID recintoId = UUID.randomUUID();
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        RecintoRepositoryPort recintoPort = mock(RecintoRepositoryPort.class);
        ValidarZonasUseCase useCase = new ValidarZonasUseCase(zonaPort, recintoPort);

        when(recintoPort.buscarPorId(recintoId)).thenReturn(Mono.just(Recinto.builder().id(recintoId).capacidadMaxima(100).build()));
        when(zonaPort.sumarCapacidadesPorRecinto(recintoId)).thenReturn(Mono.just(80));

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectNext(true)
                .verifyComplete();
    }
}

