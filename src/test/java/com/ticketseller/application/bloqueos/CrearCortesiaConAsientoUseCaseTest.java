package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCortesiaConAsientoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private CortesiaTicketCreator cortesiaTicketCreator;
    private CrearCortesiaConAsientoUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
        cortesiaTicketCreator = mock(CortesiaTicketCreator.class);
        useCase = new CrearCortesiaConAsientoUseCase(asientoRepositoryPort, bloqueoRepositoryPort,
                asientoHoldRepositoryPort, cortesiaTicketCreator);
    }

    @Test
    void asientoDisponibleBloqueaYDelegaCreacion() {
        Asiento disponible = Asiento.builder().id(asientoId).zonaId(zonaId).estado(EstadoAsiento.DISPONIBLE).build();
        Cortesia cortesia = buildCortesia(asientoId, UUID.randomUUID());

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(disponible));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId, eventoId)).thenReturn(Mono.empty());
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId, eventoId)).thenReturn(Mono.empty());
        when(bloqueoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cortesiaTicketCreator.crear(eq(eventoId), eq("Invitado VIP"), eq(CategoriaCortesia.PATROCINADOR),
                eq(zonaId), eq(disponible))).thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado VIP", CategoriaCortesia.PATROCINADOR, asientoId))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && asientoId.equals(c.getAsientoId()))
                .verifyComplete();

        verify(bloqueoRepositoryPort).guardar(any());
        verify(cortesiaTicketCreator).crear(any(), any(), any(), any(), any());
    }

    @Test
    void asientoNoDisponibleLanzaExcepcion() {
        Asiento enMantenimiento = Asiento.builder().id(asientoId).estado(EstadoAsiento.MANTENIMIENTO).build();
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(enMantenimiento));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado", CategoriaCortesia.ARTISTA, asientoId))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(cortesiaTicketCreator, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void asientoNoEncontradoLanzaExcepcion() {
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado", CategoriaCortesia.ARTISTA, asientoId))
                .expectError(AsientoNotFoundException.class)
                .verify();

        verify(cortesiaTicketCreator, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void asientoYaBloqueadoLanzaExcepcion() {
        Asiento disponible = Asiento.builder().id(asientoId).zonaId(zonaId).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(disponible));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId, eventoId))
                .thenReturn(Mono.just(com.ticketseller.domain.model.bloqueos.Bloqueo.builder().build()));
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado", CategoriaCortesia.ARTISTA, asientoId))
                .expectError(AsientoYaBloqueadoException.class)
                .verify();

        verify(cortesiaTicketCreator, never()).crear(any(), any(), any(), any(), any());
    }

    private Cortesia buildCortesia(UUID asientoIdRef, UUID ticketIdRef) {
        return Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .asientoId(asientoIdRef)
                .destinatario("Invitado VIP")
                .categoria(CategoriaCortesia.PATROCINADOR)
                .ticketId(ticketIdRef)
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
