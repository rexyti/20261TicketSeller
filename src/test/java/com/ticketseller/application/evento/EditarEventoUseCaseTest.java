package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.evento.EventoEnProgresoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EditarEventoUseCaseTest {

    @Test
    void deberiaFallarSiNoExiste() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        EditarEventoUseCase useCase = new EditarEventoUseCase(repositoryPort);

        when(repositoryPort.buscarPorId(any(UUID.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(UUID.randomUUID(), Evento.builder().build()))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiEstaEnProgreso() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        EditarEventoUseCase useCase = new EditarEventoUseCase(repositoryPort);

        UUID id = UUID.randomUUID();
        when(repositoryPort.buscarPorId(id)).thenReturn(Mono.just(Evento.builder().id(id).estado(EstadoEvento.EN_PROGRESO).build()));

        StepVerifier.create(useCase.ejecutar(id, Evento.builder().build()))
                .expectError(EventoEnProgresoException.class)
                .verify();
    }

    @Test
    void deberiaEditarEventoValido() {
        EventoRepositoryPort repositoryPort = mock(EventoRepositoryPort.class);
        EditarEventoUseCase useCase = new EditarEventoUseCase(repositoryPort);

        UUID id = UUID.randomUUID();
        Evento actual = Evento.builder()
                .id(id)
                .nombre("Concierto")
                .tipo("MUSICAL")
                .recintoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().plusDays(3))
                .fechaFin(LocalDateTime.now().plusDays(4))
                .estado(EstadoEvento.ACTIVO)
                .build();

        Evento cambios = Evento.builder().nombre("Concierto Editado").build();

        when(repositoryPort.buscarPorId(id)).thenReturn(Mono.just(actual));
        when(repositoryPort.buscarEventosSolapados(any(), any(), any())).thenReturn(Flux.empty());
        when(repositoryPort.guardar(any(Evento.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, cambios))
                .expectNextMatches(evento -> "Concierto Editado".equals(evento.getNombre()))
                .verifyComplete();
    }
}

