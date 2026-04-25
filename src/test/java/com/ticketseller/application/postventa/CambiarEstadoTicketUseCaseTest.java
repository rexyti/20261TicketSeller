package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.TransicionEstadoInvalidaException;
import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.HistorialTicketRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoTicketUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private HistorialTicketRepositoryPort historialRepositoryPort;
    @Mock
    private NotificacionEmailPort notificacionEmailPort;
    @Mock
    private VentaRepositoryPort ventaRepositoryPort;

    private CambiarEstadoTicketUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CambiarEstadoTicketUseCase(ticketRepositoryPort, historialRepositoryPort, notificacionEmailPort, ventaRepositoryPort);
    }

    @Test
    void cambiarEstadoExitoso() {
        UUID ticketId = UUID.randomUUID();
        UUID agenteId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).estado(EstadoTicket.VENDIDO).build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));
        when(historialRepositoryPort.save(any())).thenReturn(Mono.just(HistorialTicket.builder().build()));

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.CANCELADO, "Motivo", agenteId))
                .expectNextCount(1)
                .verifyComplete();

        verify(historialRepositoryPort).save(argThat(h -> h.getEstadoNuevo() == EstadoTicket.CANCELADO));
    }

    @Test
    void notificarCuandoEsAnulado() {
        UUID ticketId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).ventaId(ventaId).estado(EstadoTicket.VENDIDO).build();
        Venta venta = Venta.builder().id(ventaId).build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket.toBuilder().estado(EstadoTicket.ANULADO).build()));
        when(historialRepositoryPort.save(any())).thenReturn(Mono.just(HistorialTicket.builder().build()));
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(notificacionEmailPort.enviarAnulacion(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.ANULADO, "Fraude", UUID.randomUUID()))
                .expectNextCount(1)
                .verifyComplete();

        verify(notificacionEmailPort).enviarAnulacion(eq(venta), any(), eq("Fraude"));
    }

    @Test
    void errorEnTransicionInvalida() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder().id(ticketId).estado(EstadoTicket.CANCELADO).build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(ticketId, EstadoTicket.VENDIDO, "Intento revertir", UUID.randomUUID()))
                .expectError(TransicionEstadoInvalidaException.class)
                .verify();
    }
}
