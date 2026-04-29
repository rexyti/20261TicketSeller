package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.TicketYaUsadoException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CancelarTicketUseCaseTest {

    @Test
    void deberiaCancelarTicketYCrearReembolsoPendiente() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        CancelarTicketUseCase useCase = new CancelarTicketUseCase(ticketRepositoryPort, asientoRepositoryPort,
                eventoRepositoryPort, reembolsoRepositoryPort);

        UUID ticketId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(eventoId)
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.valueOf(120))
                .build();
        Evento evento = Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(1)).build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.cancelarTicket(ticketId))
                .assertNext(resultado -> {
                    assert resultado.ticketsCancelados().size() == 1;
                    assert resultado.montoPendiente().compareTo(BigDecimal.valueOf(120)) == 0;
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiTicketYaFueUsado() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        CancelarTicketUseCase useCase = new CancelarTicketUseCase(ticketRepositoryPort, asientoRepositoryPort,
                eventoRepositoryPort, reembolsoRepositoryPort);

        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .eventoId(UUID.randomUUID())
                .ventaId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.USADO)
                .precio(BigDecimal.TEN)
                .build();
        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.cancelarTicket(ticketId))
                .expectError(TicketYaUsadoException.class)
                .verify();
    }
}

