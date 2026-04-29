package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.TransicionEstadoInvalidaException;
import com.ticketseller.domain.model.postventa.HistorialEstadoTicket;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.HistorialEstadoTicketRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CambiarEstadoTicketUseCaseTest {

    @Test
    void deberiaCambiarEstadoYGuardarHistorial() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        HistorialEstadoTicketRepositoryPort historialRepositoryPort = mock(HistorialEstadoTicketRepositoryPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        CambiarEstadoTicketUseCase useCase = new CambiarEstadoTicketUseCase(ticketRepositoryPort, historialRepositoryPort,
                notificacionEmailPort);

        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.TEN)
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(historialRepositoryPort.guardar(any(HistorialEstadoTicket.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(notificacionEmailPort.enviarCancelacionTicket(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.CANCELADO, "Cliente solicitó devolución", UUID.randomUUID()))
                .expectNextMatches(actualizado -> EstadoTicket.CANCELADO.equals(actualizado.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaFallarConTransicionInvalida() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        HistorialEstadoTicketRepositoryPort historialRepositoryPort = mock(HistorialEstadoTicketRepositoryPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        CambiarEstadoTicketUseCase useCase = new CambiarEstadoTicketUseCase(ticketRepositoryPort, historialRepositoryPort,
                notificacionEmailPort);

        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.REEMBOLSADO)
                .precio(BigDecimal.TEN)
                .build();
        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.VENDIDO, "Revertir", UUID.randomUUID()))
                .expectError(TransicionEstadoInvalidaException.class)
                .verify();
    }
}

