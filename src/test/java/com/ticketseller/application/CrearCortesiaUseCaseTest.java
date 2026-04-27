package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoOcupadoException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.CategoriaCortesia;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CrearCortesiaUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private TicketRepositoryPort ticketRepositoryPort;
    private CodigoQrPort codigoQrPort;
    private CrearCortesiaUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        codigoQrPort = mock(CodigoQrPort.class);
        useCase = new CrearCortesiaUseCase(asientoRepositoryPort, cortesiaRepositoryPort, ticketRepositoryPort, codigoQrPort);
    }

    @Test
    void creaCortesiaConAsientoExitosamente() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        
        Asiento asiento = Asiento.builder().id(asientoId).zonaId(zonaId).estado(EstadoAsiento.DISPONIBLE).build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.guardar(any(Asiento.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(codigoQrPort.generarCodigo(anyString())).thenReturn("QR_CODE");
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cortesiaRepositoryPort.guardar(any(Cortesia.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado VIP", CategoriaCortesia.PRENSA, asientoId, zonaId))
                .expectNextMatches(cortesia -> "Invitado VIP".equals(cortesia.getDestinatario()) &&
                        cortesia.getAsientoId().equals(asientoId) &&
                        cortesia.getTicketId() != null)
                .verifyComplete();

        verify(asientoRepositoryPort).guardar(any(Asiento.class));
        verify(ticketRepositoryPort).guardar(any(Ticket.class));
    }

    @Test
    void creaCortesiaSinAsientoExitosamente() {
        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        when(codigoQrPort.generarCodigo(anyString())).thenReturn("QR_CODE");
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cortesiaRepositoryPort.guardar(any(Cortesia.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, "Prensa General", CategoriaCortesia.PRENSA, null, zonaId))
                .expectNextMatches(cortesia -> cortesia.getAsientoId() == null &&
                        "Prensa General".equals(cortesia.getDestinatario()))
                .verifyComplete();

        verify(asientoRepositoryPort, never()).buscarPorId(any());
        verify(asientoRepositoryPort, never()).guardar(any());
        verify(ticketRepositoryPort).guardar(any(Ticket.class));
    }

    @Test
    void rechazaCreacionSiAsientoEstaOcupado() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        
        Asiento asiento = Asiento.builder().id(asientoId).zonaId(zonaId).estado(EstadoAsiento.VENDIDO).build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(eventoId, "Invitado VIP", CategoriaCortesia.ARTISTA, asientoId, zonaId))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(ticketRepositoryPort, never()).guardar(any(Ticket.class));
        verify(cortesiaRepositoryPort, never()).guardar(any(Cortesia.class));
    }
}
