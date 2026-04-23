package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.EventoSolapamientoException;
import com.ticketseller.domain.exception.RecintoNoDisponibleException;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegistrarEventoUseCaseTest {

    @Test
    void deberiaRegistrarEventoValido() {
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        RecintoRepositoryPort recintoRepositoryPort = mock(RecintoRepositoryPort.class);
        RegistrarEventoUseCase useCase = new RegistrarEventoUseCase(eventoRepositoryPort, recintoRepositoryPort);

        Evento request = Evento.builder()
                .nombre("Concierto A")
                .tipo("MUSICAL")
                .recintoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().plusDays(5))
                .fechaFin(LocalDateTime.now().plusDays(6))
                .build();

        when(recintoRepositoryPort.buscarPorId(request.getRecintoId()))
                .thenReturn(Mono.just(Recinto.builder().id(request.getRecintoId()).activo(true).build()));
        when(eventoRepositoryPort.buscarEventosSolapados(any(), any(), any())).thenReturn(Flux.empty());
        when(eventoRepositoryPort.guardar(any(Evento.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(request))
                .assertNext(saved -> {
                    assert saved.getId() != null;
                    assert saved.getEstado() == EstadoEvento.ACTIVO;
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarCuandoRecintoNoDisponible() {
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        RecintoRepositoryPort recintoRepositoryPort = mock(RecintoRepositoryPort.class);
        RegistrarEventoUseCase useCase = new RegistrarEventoUseCase(eventoRepositoryPort, recintoRepositoryPort);

        Evento request = Evento.builder()
                .nombre("Concierto A")
                .tipo("MUSICAL")
                .recintoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().plusDays(5))
                .fechaFin(LocalDateTime.now().plusDays(6))
                .build();

        when(recintoRepositoryPort.buscarPorId(request.getRecintoId())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(RecintoNoDisponibleException.class)
                .verify();
    }

    @Test
    void deberiaFallarCuandoHaySolapamiento() {
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        RecintoRepositoryPort recintoRepositoryPort = mock(RecintoRepositoryPort.class);
        RegistrarEventoUseCase useCase = new RegistrarEventoUseCase(eventoRepositoryPort, recintoRepositoryPort);

        Evento request = Evento.builder()
                .nombre("Concierto A")
                .tipo("MUSICAL")
                .recintoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().plusDays(5))
                .fechaFin(LocalDateTime.now().plusDays(6))
                .build();

        when(recintoRepositoryPort.buscarPorId(request.getRecintoId()))
                .thenReturn(Mono.just(Recinto.builder().id(request.getRecintoId()).activo(true).build()));
        when(eventoRepositoryPort.buscarEventosSolapados(any(), any(), any()))
                .thenReturn(Flux.just(Evento.builder().id(UUID.randomUUID()).build()));

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(EventoSolapamientoException.class)
                .verify();
    }
}

