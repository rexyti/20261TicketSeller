package com.ticketseller.application;

import com.ticketseller.application.checkout.ConsultarEstadoTicketUseCase;
import com.ticketseller.domain.exception.venta.TicketNotFoundException;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEstadoTicketUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;

    private ConsultarEstadoTicketUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarEstadoTicketUseCase(ticketRepositoryPort);
    }

    @Test
    void debeRetornarEstadoTicketCuandoExiste() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .estado(EstadoTicket.VENDIDO)
                .categoria("VIP")
                .bloque("A")
                .coordenadaAcceso("Norte")
                .eventoId(UUID.randomUUID())
                .fechaEvento(LocalDateTime.now())
                .build();

        when(ticketRepositoryPort.findById(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(ticketId))
                .expectNextMatches(response ->
                        response.getId().equals(ticketId)
                                && response.getEstado().equals(EstadoTicket.VENDIDO)
                                && response.getCategoria().equals("VIP")
                                && response.getBloque().equals("A")
                                && response.getCoordenadaAcceso().equals("Norte")
                )
                .verifyComplete();
    }

    @Test
    void debeLanzarExcepcionCuandoTicketNoExiste() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepositoryPort.findById(ticketId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId))
                .expectError(TicketNotFoundException.class)
                .verify();
    }
}
