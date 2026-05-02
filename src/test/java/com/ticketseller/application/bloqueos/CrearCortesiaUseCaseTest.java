package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCortesiaUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private TicketRepositoryPort ticketRepositoryPort;
    private CrearCortesiaUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        useCase = new CrearCortesiaUseCase(asientoRepositoryPort, cortesiaRepositoryPort, ticketRepositoryPort);
    }

    @Test
    void cortesiaConAsientoDisponibleCreaTicketYBloquea() {
        Asiento disponible = buildAsiento(asientoId, EstadoAsiento.DISPONIBLE);
        Asiento bloqueado = disponible.toBuilder().estado(EstadoAsiento.BLOQUEADO).build();
        Ticket ticket = buildTicket();
        Cortesia cortesia = buildCortesia(asientoId, ticket.getId());

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(disponible));
        when(asientoRepositoryPort.guardar(any())).thenReturn(Mono.just(bloqueado));
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado VIP", CategoriaCortesia.PATROCINADOR, asientoId))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && c.getCodigoUnico() != null
                        && asientoId.equals(c.getAsientoId()))
                .verifyComplete();

        verify(asientoRepositoryPort).guardar(any());
        verify(ticketRepositoryPort).guardar(any());
    }

    @Test
    void cortesiaSinAsientoNoCrearTicket() {
        Cortesia cortesia = buildCortesia(null, null);
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Prensa ABC", CategoriaCortesia.PRENSA, null))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && c.getCodigoUnico() != null)
                .verifyComplete();

        verify(asientoRepositoryPort, never()).buscarPorId(any());
        verify(ticketRepositoryPort, never()).guardar(any());
    }

    @Test
    void cortesiaConAsientoOcupadoLanzaExcepcion() {
        Asiento ocupado = buildAsiento(asientoId, EstadoAsiento.OCUPADO);
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado", CategoriaCortesia.ARTISTA, asientoId))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardar(any());
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
                .destinatario("Invitado")
                .categoria(CategoriaCortesia.PATROCINADOR)
                .codigoUnico(UUID.randomUUID().toString())
                .ticketId(ticketIdRef)
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
