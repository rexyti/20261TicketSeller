package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.TipoZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCortesiaGeneralUseCaseTest {

    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private TicketRepositoryPort ticketRepositoryPort;
    private ZonaRepositoryPort zonaRepositoryPort;
    private CompuertaRepositoryPort compuertaRepositoryPort;
    private EventoRepositoryPort eventoRepositoryPort;
    private CrearCortesiaGeneralUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        eventoRepositoryPort = mock(EventoRepositoryPort.class);
        useCase = new CrearCortesiaGeneralUseCase(cortesiaRepositoryPort, ticketRepositoryPort,
                zonaRepositoryPort, compuertaRepositoryPort, eventoRepositoryPort);
    }

    @Test
    void creaTicketYCortesiaSinAsiento() {
        Ticket ticket = buildTicket(zonaId);
        Cortesia cortesia = buildCortesia(ticket.getId());
        Zona zona = Zona.builder().id(zonaId).nombre("General Zone").tipo(TipoZona.GENERAL).build();
        Evento evento = Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(3)).build();
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).nombre("Puerta Sur").build();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.just(compuerta));
        when(ticketRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.empty());
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
        Evento evento = Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(3)).build();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
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
