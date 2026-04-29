package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcesarReembolsoMasivoUseCaseTest {

    @Test
    void deberiaProcesarReembolsosMasivos() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        ProcesarReembolsoMasivoUseCase useCase = new ProcesarReembolsoMasivoUseCase(ticketRepositoryPort, reembolsoRepositoryPort);

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
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .verifyComplete();
    }
}

