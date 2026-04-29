package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.TransicionEstadoInvalidaException;
import com.ticketseller.domain.model.postventa.HistorialEstadoTicket;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.HistorialEstadoTicketRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoTicketUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private HistorialEstadoTicketRepositoryPort historialEstadoTicketRepositoryPort;
    @Mock
    private NotificacionEmailPort notificacionEmailPort;
    @InjectMocks
    private CambiarEstadoTicketUseCase useCase;

    @Test
    void deberiaCambiarEstadoYGuardarHistorial() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticketVendido(ticketId);

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(historialEstadoTicketRepositoryPort.guardar(any(HistorialEstadoTicket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.CANCELADO, "Cliente solicitó devolución", UUID.randomUUID()))
                .expectNextMatches(t -> EstadoTicket.CANCELADO.equals(t.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaFallarConTransicionInvalida() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = ticketReembolsado(ticketId);

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.VENDIDO, "Revertir", UUID.randomUUID()))
                .expectError(TransicionEstadoInvalidaException.class)
                .verify();
    }

    private Ticket ticketVendido(UUID id) {
        return Ticket.builder()
                .id(id)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.TEN)
                .build();
    }

    private Ticket ticketReembolsado(UUID id) {
        return Ticket.builder()
                .id(id)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.REEMBOLSADO)
                .precio(BigDecimal.TEN)
                .build();
    }
}
