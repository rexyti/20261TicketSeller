package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaInvalidaException;
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
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CortesiaTicketCreatorTest {

    private TicketRepositoryPort ticketRepositoryPort;
    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private ZonaRepositoryPort zonaRepositoryPort;
    private CompuertaRepositoryPort compuertaRepositoryPort;
    private EventoRepositoryPort eventoRepositoryPort;
    private CodigoQrPort codigoQrPort;
    private NotificacionEmailPort notificacionEmailPort;
    private CortesiaTicketCreator creator;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        eventoRepositoryPort = mock(EventoRepositoryPort.class);
        codigoQrPort = mock(CodigoQrPort.class);
        notificacionEmailPort = mock(NotificacionEmailPort.class);
        creator = new CortesiaTicketCreator(ticketRepositoryPort, cortesiaRepositoryPort,
                zonaRepositoryPort, compuertaRepositoryPort, eventoRepositoryPort, codigoQrPort,
                notificacionEmailPort);
    }

    @Test
    void crearGeneraQrYGuardaTicketYCortesiaSinAsiento() {
        UUID ticketId = UUID.randomUUID();
        String qrBase64 = "data:image/png;base64,qrdata==";
        Ticket sinQr = buildTicket(ticketId, null, null);
        Ticket conQr = buildTicket(ticketId, qrBase64, null);
        Cortesia cortesia = buildCortesia(ticketId, null);
        Zona zona = Zona.builder().id(zonaId).nombre("VIP Zone").tipo(TipoZona.VIP).build();
        Evento evento = Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(5)).build();
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).nombre("Puerta Norte").build();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.just(compuerta));
        when(ticketRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.empty());
        when(ticketRepositoryPort.guardar(argThat(t -> t != null && t.getCodigoQr() == null))).thenReturn(Mono.just(sinQr));
        when(codigoQrPort.generarCodigo(eq(ticketId.toString()))).thenReturn(qrBase64);
        when(ticketRepositoryPort.guardar(argThat(t -> t != null && t.getCodigoQr() != null))).thenReturn(Mono.just(conQr));
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));
        when(notificacionEmailPort.enviarCortesiaGenerada(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(creator.crear(eventoId, null, "prensa@abc.com", CategoriaCortesia.PRENSA, zonaId, null))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && c.getTicketId() != null
                        && c.getAsientoId() == null)
                .verifyComplete();

        verify(codigoQrPort).generarCodigo(ticketId.toString());
        verify(ticketRepositoryPort, times(2)).guardar(any());
        verify(cortesiaRepositoryPort).guardar(any());
    }

    @Test
    void crearConAsientoIncluiAsientoEnTicketYCortesia() {
        UUID ticketId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        String qrBase64 = "data:image/png;base64,qrdata==";
        Asiento asiento = Asiento.builder().id(asientoId).zonaId(zonaId).estado(EstadoAsiento.DISPONIBLE)
                .numero("A1").build();
        Ticket sinQr = buildTicket(ticketId, null, asientoId);
        Ticket conQr = buildTicket(ticketId, qrBase64, asientoId);
        Cortesia cortesia = buildCortesia(ticketId, asientoId);
        Zona zona = Zona.builder().id(zonaId).nombre("VIP Zone").tipo(TipoZona.VIP).build();
        Evento evento = Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(5)).build();
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).nombre("Puerta Norte").build();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.just(compuerta));
        when(ticketRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.empty());
        when(ticketRepositoryPort.guardar(argThat(t -> t != null && t.getCodigoQr() == null))).thenReturn(Mono.just(sinQr));
        when(codigoQrPort.generarCodigo(eq(ticketId.toString()))).thenReturn(qrBase64);
        when(ticketRepositoryPort.guardar(argThat(t -> t != null && t.getCodigoQr() != null))).thenReturn(Mono.just(conQr));
        when(cortesiaRepositoryPort.guardar(any())).thenReturn(Mono.just(cortesia));
        when(notificacionEmailPort.enviarCortesiaGenerada(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(creator.crear(eventoId, null, "invitado@vip.com", CategoriaCortesia.PATROCINADOR, zonaId, asiento))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && asientoId.equals(c.getAsientoId()))
                .verifyComplete();

        verify(codigoQrPort).generarCodigo(ticketId.toString());
    }

    @Test
    void zonaIdNulaLanzaZonaInvalidaException() {
        Evento evento = Evento.builder().id(eventoId).build();
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));

        StepVerifier.create(creator.crear(eventoId, null, "invitado@test.com", CategoriaCortesia.ARTISTA, null, null))
                .expectError(ZonaInvalidaException.class)
                .verify();
    }

    @Test
    void eventoNoEncontradoLanzaEventoNotFoundException() {
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(creator.crear(eventoId, null, "invitado@test.com", CategoriaCortesia.PRENSA, zonaId, null))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    private Ticket buildTicket(UUID id, String codigoQr, UUID asientoId) {
        return Ticket.builder()
                .id(id)
                .eventoId(eventoId)
                .zonaId(zonaId)
                .asientoId(asientoId)
                .codigoQr(codigoQr)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .build();
    }

    private Cortesia buildCortesia(UUID ticketId, UUID asientoId) {
        return Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .asientoId(asientoId)
                .emailDestinatario("prensa@abc.com")
                .categoria(CategoriaCortesia.PRENSA)
                .ticketId(ticketId)
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
