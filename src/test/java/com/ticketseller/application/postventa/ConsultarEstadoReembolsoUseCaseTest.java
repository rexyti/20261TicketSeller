package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarEstadoReembolsoUseCaseTest {

    @Test
    void deberiaConsultarTicketsConEstadoDeReembolso() {
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        ConsultarEstadoReembolsoUseCase useCase = new ConsultarEstadoReembolsoUseCase(ventaRepositoryPort,
                ticketRepositoryPort, reembolsoRepositoryPort);

        UUID compradorId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        Venta venta = Venta.builder()
                .id(ventaId)
                .compradorId(compradorId)
                .eventoId(UUID.randomUUID())
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .total(BigDecimal.TEN)
                .build();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(ventaId)
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.TEN)
                .build();
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .ventaId(ventaId)
                .monto(BigDecimal.TEN)
                .estado(EstadoReembolso.PENDIENTE)
                .build();

        when(ventaRepositoryPort.buscarPorCompradorId(compradorId)).thenReturn(Flux.just(venta));
        when(ticketRepositoryPort.buscarPorVentaIds(any())).thenReturn(Flux.fromIterable(List.of(ticket)));
        when(reembolsoRepositoryPort.buscarPorTicketId(ticketId)).thenReturn(Mono.just(reembolso));

        StepVerifier.create(useCase.ejecutar(compradorId))
                .expectNextMatches(item -> item.reembolso() != null && EstadoReembolso.PENDIENTE.equals(item.reembolso().getEstado()))
                .verifyComplete();
    }
}

