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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelarTicketUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private AsientoRepositoryPort asientoRepositoryPort;
    @Mock
    private EventoRepositoryPort eventoRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;
    @InjectMocks
    private CancelarTicketUseCase useCase;

    @Test
    void deberiaCancelarTicketYCrearReembolsoPendiente() {
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
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.cancelarTicket(ticketId))
                .assertNext(resultado -> {
                    assert resultado.ticketsCancelados().size() == 1;
                    assert resultado.montoPendiente().compareTo(BigDecimal.valueOf(120)) == 0;
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiTicketYaFueUsado() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
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
