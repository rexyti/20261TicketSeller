package com.ticketseller.application;

import com.ticketseller.domain.exception.CancelacionFueraDePlazoException;
import com.ticketseller.domain.exception.TicketYaUsadoException;
import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelarTicketUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private AsientoRepositoryPort asientoRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;
    @Mock
    private EventoRepositoryPort eventoRepositoryPort;

    private CancelarTicketUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelarTicketUseCase(ticketRepositoryPort, asientoRepositoryPort, reembolsoRepositoryPort, eventoRepositoryPort);
    }

    @Test
    void cancelarTicketExitoso() {
        UUID ticketId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .eventoId(eventoId)
                .asientoId(asientoId)
                .ventaId(ventaId)
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.valueOf(100))
                .build();

        Evento evento = Evento.builder()
                .id(eventoId)
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .build();

        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.VENDIDO)
                .build();

        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .monto(BigDecimal.valueOf(100))
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.guardar(any())).thenReturn(Mono.just(asiento));
        when(reembolsoRepositoryPort.save(any())).thenReturn(Mono.just(reembolso));

        StepVerifier.create(useCase.ejecutar(List.of(ticketId)))
                .expectNextMatches(response -> response.ticketsCancelados().contains(ticketId))
                .verifyComplete();

        verify(ticketRepositoryPort).guardar(argThat(t -> t.getEstado() == EstadoTicket.CANCELADO));
        verify(asientoRepositoryPort).guardar(argThat(a -> a.getEstado() == EstadoAsiento.DISPONIBLE));
        verify(reembolsoRepositoryPort).save(any());
    }

    @Test
    void errorSiTicketYaUsado() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .estado(EstadoTicket.USADO)
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(List.of(ticketId)))
                .expectError(TicketYaUsadoException.class)
                .verify();
    }

    @Test
    void errorSiEventoYaOcurrio() {
        UUID ticketId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .eventoId(eventoId)
                .estado(EstadoTicket.VENDIDO)
                .build();
        Evento evento = Evento.builder()
                .id(eventoId)
                .fechaInicio(LocalDateTime.now().minusHours(1))
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));

        StepVerifier.create(useCase.ejecutar(List.of(ticketId)))
                .expectError(CancelacionFueraDePlazoException.class)
                .verify();
    }
}
