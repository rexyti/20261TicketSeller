package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.TipoZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCortesiaConAsientoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private TicketRepositoryPort ticketRepositoryPort;
    private ZonaRepositoryPort zonaRepositoryPort;
    private CompuertaRepositoryPort compuertaRepositoryPort;
    private EventoRepositoryPort eventoRepositoryPort;
    private CrearCortesiaConAsientoUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        eventoRepositoryPort = mock(EventoRepositoryPort.class);
        useCase = new CrearCortesiaConAsientoUseCase(asientoRepositoryPort, cortesiaRepositoryPort,
                ticketRepositoryPort, zonaRepositoryPort, compuertaRepositoryPort, eventoRepositoryPort);
    }

    @Test
    void asientoDisponibleCreaTicketBloqueasientoYGuardaCortesia() {
        Asiento disponible = buildAsiento(asientoId, EstadoAsiento.DISPONIBLE);
        Asiento bloqueado = disponible.toBuilder().estado(EstadoAsiento.BLOQUEADO).build();
        Ticket ticket = buildTicket();
        Cortesia cortesia = buildCortesia(asientoId, ticket.getId());
        Zona zona = Zona.builder().id(zonaId).nombre("VIP Zone").tipo(TipoZona.VIP).build();
        Evento evento = Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(5)).build();
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).nombre("Puerta Norte").build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(disponible));
        when(asientoRepositoryPort.guardar(any())).thenReturn(Mono.just(bloqueado));
        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.just(compuerta));
        when(ticketRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.empty());
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado VIP", CategoriaCortesia.PATROCINADOR, asientoId))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && c.getCodigoUnico() != null
                        && asientoId.equals(c.getAsientoId()))
                .verifyComplete();

        verify(asientoRepositoryPort).guardar(any());
        verify(ticketRepositoryPort).guardar(any());
        verify(cortesiaRepositoryPort).guardar(any());
    }

    @Test
    void asientoOcupadoLanzaExcepcion() {
        Asiento ocupado = buildAsiento(asientoId, EstadoAsiento.OCUPADO);
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado", CategoriaCortesia.ARTISTA, asientoId))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardar(any());
        verify(ticketRepositoryPort, never()).guardar(any());
        verify(cortesiaRepositoryPort, never()).guardar(any());
    }

    @Test
    void asientoNoEncontradoLanzaExcepcion() {
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado", CategoriaCortesia.ARTISTA, asientoId))
                .expectError(AsientoNotFoundException.class)
                .verify();

        verify(ticketRepositoryPort, never()).guardar(any());
        verify(cortesiaRepositoryPort, never()).guardar(any());
    }

    private Asiento buildAsiento(UUID id, EstadoAsiento estado) {
        return Asiento.builder().id(id).zonaId(zonaId).estado(estado).build();
    }

    private Ticket buildTicket() {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .zonaId(zonaId)
                .asientoId(asientoId)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .build();
    }

    private Cortesia buildCortesia(UUID asientoIdRef, UUID ticketIdRef) {
        return Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .asientoId(asientoIdRef)
                .destinatario("Invitado VIP")
                .categoria(CategoriaCortesia.PATROCINADOR)
                .codigoUnico(UUID.randomUUID().toString())
                .ticketId(ticketIdRef)
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
