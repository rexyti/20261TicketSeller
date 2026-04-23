package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancelarEventoUseCaseTest {

    @Test
    void deberiaFallarSiNoExiste() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        CancelarEventoUseCase useCase = new CancelarEventoUseCase(repositoryPort);

        when(repositoryPort.buscarPorId(any(UUID.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(UUID.randomUUID(), "Fuerza mayor"))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    @Test
    void deberiaCancelarEvento() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        CancelarEventoUseCase useCase = new CancelarEventoUseCase(repositoryPort);

        UUID id = UUID.randomUUID();
        when(repositoryPort.buscarPorId(id)).thenReturn(Mono.just(Evento.builder().id(id).estado(EstadoEvento.ACTIVO).build()));
        when(repositoryPort.guardar(any(Evento.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, "Fuerza mayor"))
                .expectNextMatches(evento -> evento.getEstado() == EstadoEvento.CANCELADO)
                .verifyComplete();
    }
}

