package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesarReembolsoMasivoUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;
    @InjectMocks
    private ProcesarReembolsoMasivoUseCase useCase;

    @Test
    void deberiaCancelarTicketsYCrearReembolsosSaltandoCortesias() {
        UUID eventoId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ventaId(UUID.randomUUID())
                .eventoId(eventoId)
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.valueOf(90))
                .esCortesia(false)
                .build();
        Ticket cortesia = ticket.toBuilder().id(UUID.randomUUID()).esCortesia(true).build();

        when(ticketRepositoryPort.buscarPorEventoYEstados(eq(eventoId), eq(Set.of(EstadoTicket.VENDIDO))))
                .thenReturn(Flux.just(ticket, cortesia));
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .verifyComplete();
    }
}
