package com.ticketseller.application.evento;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarEventosUseCaseTest {

    @Test
    void deberiaListarActivosPorDefecto() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        ListarEventosUseCase useCase = new ListarEventosUseCase(repositoryPort);

        when(repositoryPort.listarActivos()).thenReturn(Flux.just(Evento.builder().nombre("E1").build()));

        StepVerifier.create(useCase.ejecutar(null))
                .expectNextMatches(evento -> "E1".equals(evento.getNombre()))
                .verifyComplete();
    }

    @Test
    void deberiaListarPorEstado() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        ListarEventosUseCase useCase = new ListarEventosUseCase(repositoryPort);

        when(repositoryPort.listarPorEstado(EstadoEvento.CANCELADO))
                .thenReturn(Flux.just(Evento.builder().estado(EstadoEvento.CANCELADO).build()));

        StepVerifier.create(useCase.ejecutar(EstadoEvento.CANCELADO))
                .expectNextMatches(evento -> evento.getEstado() == EstadoEvento.CANCELADO)
                .verifyComplete();
    }
}

