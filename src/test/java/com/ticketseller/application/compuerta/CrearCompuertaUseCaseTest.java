package com.ticketseller.application.compuerta;

import com.ticketseller.domain.exception.CompuertaInvalidaException;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCompuertaUseCaseTest {

    @Test
    void deberiaCrearCompuertaGeneral() {
        UUID recintoId = UUID.randomUUID();
        CompuertaRepositoryPort compuertaPort = mock(CompuertaRepositoryPort.class);
        RecintoRepositoryPort recintoPort = mock(RecintoRepositoryPort.class);
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        CrearCompuertaUseCase useCase = new CrearCompuertaUseCase(compuertaPort, recintoPort, zonaPort);

        when(recintoPort.buscarPorId(recintoId)).thenReturn(Mono.just(Recinto.builder().id(recintoId).build()));
        when(compuertaPort.guardar(any(Compuerta.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(recintoId, Compuerta.builder().nombre("Puerta A").build()))
                .expectNextMatches(Compuerta::isEsGeneral)
                .verifyComplete();
    }

    @Test
    void deberiaCrearCompuertaConZona() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        CompuertaRepositoryPort compuertaPort = mock(CompuertaRepositoryPort.class);
        RecintoRepositoryPort recintoPort = mock(RecintoRepositoryPort.class);
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        CrearCompuertaUseCase useCase = new CrearCompuertaUseCase(compuertaPort, recintoPort, zonaPort);

        when(recintoPort.buscarPorId(recintoId)).thenReturn(Mono.just(Recinto.builder().id(recintoId).build()));
        when(zonaPort.buscarPorId(zonaId)).thenReturn(Mono.just(Zona.builder().id(zonaId).recintoId(recintoId).build()));
        when(compuertaPort.guardar(any(Compuerta.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(recintoId, Compuerta.builder().nombre("Puerta B").zonaId(zonaId).build()))
                .expectNextMatches(c -> !c.isEsGeneral() && zonaId.equals(c.getZonaId()))
                .verifyComplete();
    }

    @Test
    void deberiaFallarCuandoCompuertaEsInvalida() {
        UUID recintoId = UUID.randomUUID();
        CompuertaRepositoryPort compuertaPort = mock(CompuertaRepositoryPort.class);
        RecintoRepositoryPort recintoPort = mock(RecintoRepositoryPort.class);
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        CrearCompuertaUseCase useCase = new CrearCompuertaUseCase(compuertaPort, recintoPort, zonaPort);

        StepVerifier.create(useCase.ejecutar(recintoId, Compuerta.builder().nombre(" ").build()))
                .expectError(CompuertaInvalidaException.class)
                .verify();

        verify(recintoPort, never()).buscarPorId(any());
    }
}

