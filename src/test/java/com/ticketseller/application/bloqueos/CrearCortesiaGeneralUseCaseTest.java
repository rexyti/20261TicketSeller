package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCortesiaGeneralUseCaseTest {

    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private TicketRepositoryPort ticketRepositoryPort;
    private CrearCortesiaGeneralUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        useCase = new CrearCortesiaGeneralUseCase(cortesiaRepositoryPort, ticketRepositoryPort);
    }

    @Test
    void creaTicketYCortesiaSinAsiento() {
        Ticket ticket = buildTicket(zonaId);
        Cortesia cortesia = buildCortesia(ticket.getId());

        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Prensa ABC", CategoriaCortesia.PRENSA, zonaId))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && c.getCodigoUnico() != null
                        && c.getTicketId() != null
                        && c.getAsientoId() == null)
                .verifyComplete();

        verify(ticketRepositoryPort).guardar(any());
        verify(cortesiaRepositoryPort).guardar(any());
    }

    @Test
    void creaTicketYCortesiaSinZona() {
        Ticket ticket = buildTicket(null);
        Cortesia cortesia = buildCortesia(ticket.getId());

        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Artista Invitado", CategoriaCortesia.ARTISTA, null))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && c.getCodigoUnico() != null
                        && c.getTicketId() != null)
                .verifyComplete();

        verify(ticketRepositoryPort).guardar(any());
        verify(cortesiaRepositoryPort).guardar(any());
    }

    private Ticket buildTicket(UUID zonaIdRef) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .zonaId(zonaIdRef)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .build();
    }

    private Cortesia buildCortesia(UUID ticketIdRef) {
        return Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .destinatario("Prensa ABC")
                .categoria(CategoriaCortesia.PRENSA)
                .codigoUnico(UUID.randomUUID().toString())
                .ticketId(ticketIdRef)
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
