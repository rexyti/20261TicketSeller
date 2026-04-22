package com.ticketseller.application.compuerta;

import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsignarCompuertaAZonaUseCaseTest {

    @Test
    void deberiaAsignarCompuertaAZona() {
        UUID compuertaId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        CompuertaRepositoryPort compuertaPort = mock(CompuertaRepositoryPort.class);
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        AsignarCompuertaAZonaUseCase useCase = new AsignarCompuertaAZonaUseCase(compuertaPort, zonaPort);

        when(compuertaPort.buscarPorId(compuertaId)).thenReturn(Mono.just(Compuerta.builder().id(compuertaId).esGeneral(true).build()));
        when(zonaPort.buscarPorId(zonaId)).thenReturn(Mono.just(Zona.builder().id(zonaId).build()));
        when(compuertaPort.guardar(any(Compuerta.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(compuertaId, zonaId))
                .expectNextMatches(c -> zonaId.equals(c.getZonaId()) && !c.isEsGeneral())
                .verifyComplete();
    }
}

