package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.CategoriaTicket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservarAsientosUseCaseTest {

    @Test
    void deberiaReservarAsientosCuandoHayDisponibilidad() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        CompuertaRepositoryPort compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);

        ReservarAsientosUseCase useCase = new ReservarAsientosUseCase(ticketRepositoryPort, ventaRepositoryPort,
                zonaRepositoryPort, precioZonaRepositoryPort, compuertaRepositoryPort, eventoRepositoryPort,
                asientoRepositoryPort);

        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(
                Zona.builder().id(zonaId).nombre("Zona Norte").capacidad(100).build()));
        when(precioZonaRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.just(
                PrecioZona.builder().id(UUID.randomUUID()).eventoId(eventoId).zonaId(zonaId).precio(BigDecimal.TEN).build()));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.just(
                Compuerta.builder().id(UUID.randomUUID()).nombre("Compuerta Norte").esGeneral(true).build()));
        when(ticketRepositoryPort.contarPorEventoYZonaYEstados(any(), any(), anySet())).thenReturn(Mono.just(1L));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(
                Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(30)).build()));
        when(ventaRepositoryPort.guardar(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(ticketRepositoryPort.guardarTodos(any())).thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));

        ReservarAsientosCommand command = new ReservarAsientosCommand(UUID.randomUUID(), eventoId, zonaId, 2, false, null);

        StepVerifier.create(useCase.ejecutar(command))
                .assertNext(detalle -> {
                    assert detalle.venta().getId() != null;
                    assert detalle.tickets().size() == 2;
                    assert detalle.tickets().get(0).getAccessDetails() != null;
                    assert detalle.tickets().get(0).getAccessDetails().getCategoria() == CategoriaTicket.GENERAL;
                    assert "Zona Norte".equals(detalle.tickets().get(0).getAccessDetails().getZona());
                    assert "Compuerta Norte".equals(detalle.tickets().get(0).getAccessDetails().getCompuerta());
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiNoHayDisponibilidad() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        CompuertaRepositoryPort compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);

        ReservarAsientosUseCase useCase = new ReservarAsientosUseCase(ticketRepositoryPort, ventaRepositoryPort,
                zonaRepositoryPort, precioZonaRepositoryPort, compuertaRepositoryPort, eventoRepositoryPort,
                asientoRepositoryPort);

        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(
                Zona.builder().id(zonaId).nombre("Zona Sur").capacidad(2).build()));
        when(precioZonaRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.just(
                PrecioZona.builder().id(UUID.randomUUID()).eventoId(eventoId).zonaId(zonaId).precio(BigDecimal.TEN).build()));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.empty());
        when(ticketRepositoryPort.contarPorEventoYZonaYEstados(any(), any(), anySet())).thenReturn(Mono.just(2L));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(
                Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(30)).build()));

        ReservarAsientosCommand command = new ReservarAsientosCommand(UUID.randomUUID(), eventoId, zonaId, 1, false, null);

        StepVerifier.create(useCase.ejecutar(command))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }
}
